package com.rural.travel.ai;

import com.rural.travel.dto.ContentGenerateDTO;
import com.rural.travel.entity.ScenicSpot;
import com.rural.travel.exception.BizException;
import com.rural.travel.security.SecurityUtils;
import com.rural.travel.service.ScenicSpotService;
import com.rural.travel.service.UserAiConfigService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/** AIGC 内容生成:大模型增强 + 模板引擎兜底(种草文案/游玩攻略/宣传标语/短视频脚本) */
@Service
public class ContentGenerateService {

    private final ScenicSpotService scenicSpotService;
    private final AiClient aiClient;
    private final UserAiConfigService userAiConfigService;

    public ContentGenerateService(ScenicSpotService scenicSpotService, AiClient aiClient,
                                  UserAiConfigService userAiConfigService) {
        this.scenicSpotService = scenicSpotService;
        this.aiClient = aiClient;
        this.userAiConfigService = userAiConfigService;
    }

    public Map<String, String> generate(ContentGenerateDTO dto) {
        ScenicSpot spot;
        if (dto.getScenicSpotId() != null) {
            spot = scenicSpotService.getById(dto.getScenicSpotId());
        } else {
            List<ScenicSpot> hot = scenicSpotService.hot(1);
            if (hot.isEmpty()) throw new BizException("暂无景区数据,请先在管理端添加景区");
            spot = hot.get(0);
        }
        scenicSpotService.enrich(List.of(spot));

        String typeName = switch (dto.getType()) {
            case "grass" -> "种草文案";
            case "guide" -> "游玩攻略";
            case "slogan" -> "宣传标语";
            case "video" -> "短视频脚本";
            default -> throw new BizException("内容类型仅支持 grass/guide/slogan/video");
        };
        String styleName = switch (dto.getStyle() == null ? "fresh" : dto.getStyle()) {
            case "lively" -> "活泼";
            case "literary" -> "文艺";
            default -> "清新";
        };

        // 大模型增强:使用当前操作者(管理员)自己的密钥,未配置则走本地模板
        Long userId = SecurityUtils.currentUserIdOrNull();
        UserAiConfigService.AiKey key = userAiConfigService.keyOf(userId);
        if (key != null) {
            String prompt = "请为乡村景区「" + spot.getName() + "」创作一篇" + typeName
                    + ",风格" + styleName + "。景区信息:特色=" + spot.getFeatures()
                    + ";标签=" + String.join("/", spot.getTagNames() == null ? List.of() : spot.getTagNames())
                    + ";门票=" + spot.getTicketPrice() + "元;简介=" + spot.getDescription()
                    + "。直接输出成稿,不要解释。";
            String content = aiClient.chatWith(key.apiKey(), key.baseUrl(), key.model(),
                    "你是资深文旅内容编辑,文案自然有画面感,不堆砌辞藻。", prompt);
            if (content != null) {
                return Map.of("title", typeName + " · " + spot.getName(), "content", content, "engine", "AI增强");
            }
        }
        // 模板引擎兜底
        return Map.of("title", typeName + " · " + spot.getName(),
                "content", byTemplate(dto.getType(), styleName, spot), "engine", "本地模板");
    }

    private String byTemplate(String type, String style, ScenicSpot s) {
        String tags = String.join(" ", (s.getTagNames() == null ? List.<String>of() : s.getTagNames())
                .stream().map(t -> "#" + t).toList());
        String price = s.getTicketPrice() == null || s.getTicketPrice().doubleValue() <= 0
                ? "免费" : s.getTicketPrice().stripTrailingZeros().toPlainString() + "元";
        String firstFeature = StringUtils.hasText(s.getFeatures())
                ? s.getFeatures().split("[,、,]")[0].trim() : "田园风光";
        String opening = switch (style) {
            case "活泼" -> "姐妹们快看过来!!";
            case "文艺" -> "山水有约,田园入梦。";
            default -> "私藏小众地分享~";
        };
        return switch (type) {
            case "grass" -> opening + "\n\n🌿「" + s.getName() + "」真的太值得来了!\n\n"
                    + (s.getDescription() == null ? "" : s.getDescription()) + "\n\n"
                    + "✨亮点:" + (s.getFeatures() == null ? firstFeature : s.getFeatures()) + "\n"
                    + "📍地址:" + (s.getAddress() == null ? (s.getProvince() + "" + s.getCity()) : s.getAddress()) + "\n"
                    + "🎫门票:" + price + "\n"
                    + "💡贴士:" + (s.getTips() == null ? "穿舒适的鞋子,慢慢逛" : s.getTips()) + "\n\n"
                    + tags + " #乡村旅游 #周末去哪儿";
            case "guide" -> "【" + s.getName() + " · 一日游攻略】\n\n"
                    + "🕘 上午:抵达后先打卡核心景观," + firstFeature + "光线最好,适合拍照。\n"
                    + "🍜 中午:在景区周边农家乐用餐,尝尝当地土菜。\n"
                    + "🌾 下午:深度游览," + (s.getDescription() == null ? "" : "「" + s.getDescription().split("。")[0] + "」") + "。\n"
                    + "🌇 傍晚:日落时分是全天最美的时候,别急着走。\n\n"
                    + "📌 实用信息\n门票:" + price + " | 开放时间:" + (s.getOpenTime() == null ? "全天" : s.getOpenTime()) + "\n"
                    + "提示:" + (s.getTips() == null ? "注意防晒补水" : s.getTips());
            case "slogan" -> "1. " + s.getName() + "——把日子过成田园诗。\n"
                    + "2. 来" + s.getName() + "," + firstFeature + "不负好时光。\n"
                    + "3. 心归田园处,自在" + s.getName() + "。\n"
                    + "4. " + s.getName() + ":看得见山,望得见水,记得住乡愁。\n"
                    + "5. 周末不远行," + s.getName() + "刚刚好。";
            case "video" -> "【" + s.getName() + " · 60秒短视频脚本】\n\n"
                    + "【0-5s 开场】航拍/全景缓推,字幕:" + s.getName() + "·" + firstFeature + "。旁白:" + opening + "\n"
                    + "【5-20s 亮点一】近景特写:" + (s.getFeatures() == null ? firstFeature : s.getFeatures().split("[,、,]")[0].trim())
                    + ",手持跟拍+慢动作。旁白:" + (s.getDescription() == null ? "" : s.getDescription().split("。")[0] + "。") + "\n"
                    + "【20-40s 亮点二】游客视角互动镜头,展现" + (s.getCategoryName() == null ? "田园氛围" : s.getCategoryName())
                    + "特色,穿插当地人笑脸。旁白:" + (s.getTips() == null ? "来这里,把节奏放慢。" : s.getTips()) + "\n"
                    + "【40-55s 亮点三】日落/夜色空镜,情绪拉满。字幕:门票" + price
                    + " | " + (s.getOpenTime() == null ? "全天开放" : s.getOpenTime()) + "\n"
                    + "【55-60s 结尾】定格logo位,口播:周末不远行," + s.getName() + "刚刚好。点击定位,出发!\n\n"
                    + "BGM建议:轻快民谣/自然白噪音 | 话题:" + tags + " #乡村旅游";
            default -> "";
        };
    }
}
