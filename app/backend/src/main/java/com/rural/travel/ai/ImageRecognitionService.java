package com.rural.travel.ai;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rural.travel.entity.ScenicImageHash;
import com.rural.travel.entity.ScenicSpot;
import com.rural.travel.exception.BizException;
import com.rural.travel.mapper.ScenicImageHashMapper;
import com.rural.travel.service.ScenicSpotService;
import com.rural.travel.utils.PerceptualHash;
import com.rural.travel.vo.RecognizeVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 拍照识景:pHash 本地匹配 + 可选多模态大模型增强,游客免登录可用 */
@Slf4j
@Service
public class ImageRecognitionService {

    /** 汉明距离判定阈值(≤10 认为同景) */
    private static final int MATCH_THRESHOLD = 10;

    private final ScenicImageHashMapper hashMapper;
    private final ScenicSpotService scenicSpotService;
    private final AiClient aiClient;
    private final RestTemplate restTemplate;

    /** 站点地址:库中图片存相对路径(如 /images/rural-1.jpg)时,拼接此前缀才能抓取计算 pHash */
    @org.springframework.beans.factory.annotation.Value("${rural.site.base-url:http://localhost:5174}")
    private String siteBaseUrl;

    /** 相对路径补全为可抓取的绝对地址 */
    private String absolutize(String url) {
        if (url == null || url.isBlank()) return null;
        if (url.startsWith("http")) return url;
        String base = siteBaseUrl == null ? "" : siteBaseUrl.replaceAll("/+$", "");
        return base + (url.startsWith("/") ? url : "/" + url);
    }

    private volatile Map<Long, String> hashCache = new HashMap<>();

    public ImageRecognitionService(ScenicImageHashMapper hashMapper, ScenicSpotService scenicSpotService,
                                   AiClient aiClient, RestTemplate restTemplate) {
        this.hashMapper = hashMapper;
        this.scenicSpotService = scenicSpotService;
        this.aiClient = aiClient;
        this.restTemplate = restTemplate;
    }

    public synchronized void refreshCache() {
        Map<Long, String> map = new HashMap<>();
        for (ScenicImageHash h : hashMapper.selectList(null)) {
            map.put(h.getScenicSpotId(), h.getHashValue());
        }
        this.hashCache = map;
    }

    public RecognizeVO recognize(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BizException("请先选择一张照片");
        String type = file.getContentType() == null ? "" : file.getContentType();
        if (!type.startsWith("image/")) throw new BizException("只支持图片文件");
        if (hashCache.isEmpty()) refreshCache();

        RecognizeVO vo = new RecognizeVO();
        try {
            byte[] bytes = file.getBytes();
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img == null) throw new BizException("无法解析这张图片,换一张试试");

            // 1) 本地 pHash 匹配
            String hash = PerceptualHash.hash(img);
            Long bestSpotId = null;
            int bestDist = Integer.MAX_VALUE;
            for (Map.Entry<Long, String> e : hashCache.entrySet()) {
                int d = PerceptualHash.hamming(hash, e.getValue());
                if (d < bestDist) { bestDist = d; bestSpotId = e.getKey(); }
            }
            if (bestSpotId != null && bestDist <= MATCH_THRESHOLD) {
                ScenicSpot spot = scenicSpotService.detail(bestSpotId);
                vo.setMatched(true);
                vo.setSpot(spot);
                vo.setConfidence(Math.round((1 - bestDist / 64.0) * 100) / 100.0);
                vo.setMessage("识别成功,这看起来是「" + spot.getName() + "」");
                return vo;
            }

            // 2) 多模态大模型增强(可选)
            if (aiClient.visionEnabled()) {
                String name = aiClient.vision(
                        "请识别这张照片拍摄的乡村景点。只回复景点名称;如果无法确定,只回复「未知」。",
                        Base64.getEncoder().encodeToString(bytes));
                if (name != null && !name.contains("未知")) {
                    for (ScenicSpot s : scenicSpotService.allActive()) {
                        if (name.contains(s.getName()) || s.getName().contains(name.trim())) {
                            vo.setMatched(true);
                            vo.setSpot(scenicSpotService.detail(s.getId()));
                            vo.setConfidence(0.85);
                            vo.setMessage("识别成功,这看起来是「" + s.getName() + "」");
                            return vo;
                        }
                    }
                }
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("图像识别异常: {}", e.getMessage());
            throw new BizException("识别服务繁忙,请稍后再试");
        }

        // 3) 未识别:给出热门替代推荐
        vo.setMatched(false);
        vo.setMessage("暂时没有认出这是哪里,可以换一张光线更好、主体更清晰的照片试试");
        vo.setAlternatives(scenicSpotService.hot(3));
        return vo;
    }

    /**
     * 启动预热:抓取封面图计算真实 pHash(失败跳过,保留种子值)。
     *
     * <p>注意:封面图是相对路径(如 /images/rural-1.jpg),必须拼 siteBaseUrl 后由前端静态服务提供。
     * 而一键启动脚本的顺序是 MySQL → 后端 → 前端,后端起来时前端往往还没监听,
     * 第一轮必然全部抓取失败(历史上表现为「成功 0/100」)。
     * 因此这里做多轮重试:只要还有失败的景区,就等待前端就绪后继续补抓,直到全成功或用尽轮次。
     */
    public void warmup() {
        List<ScenicSpot> spots = scenicSpotService.allActive();
        int total = spots.size();
        int ok = 0;

        // 最多重试 6 轮,每轮间隔 10 秒(等前端 Vite 起来)
        for (int round = 1; round <= 6; round++) {
            ok = warmupOnce(spots);
            refreshCache();
            if (ok >= total) break;
            if (round < 6) {
                log.info("封面图哈希预热第 {} 轮完成 {}/{},等待前端静态服务就绪后重试...", round, ok, total);
                try {
                    Thread.sleep(10_000L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        log.info("识景哈希预热完成,成功 {}/{} 个景区", ok, total);
    }

    /** 执行一轮抓取,返回成功数量 */
    private int warmupOnce(List<ScenicSpot> spots) {
        int ok = 0;
        for (ScenicSpot s : spots) {
            String url = absolutize(s.getCoverImage());
            if (url == null || !url.startsWith("http")) continue;
            try {
                byte[] bytes = restTemplate.getForObject(url, byte[].class);
                if (bytes == null) continue;
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
                if (img == null) continue;
                String hash = PerceptualHash.hash(img);
                hashMapper.delete(new QueryWrapper<ScenicImageHash>().eq("scenic_spot_id", s.getId()));
                ScenicImageHash row = new ScenicImageHash();
                row.setScenicSpotId(s.getId());
                row.setImageUrl(url);
                row.setHashValue(hash);
                hashMapper.insert(row);
                ok++;
            } catch (Exception e) {
                log.debug("封面图哈希预热跳过 {}: {}", s.getName(), e.getMessage());
            }
        }
        return ok;
    }
}
