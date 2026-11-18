package com.rural.travel.guide;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rural.travel.ai.AiClient;
import com.rural.travel.guide.dto.ContentBatchDTO;
import com.rural.travel.guide.entity.KgEntity;
import com.rural.travel.guide.entity.MmContent;
import com.rural.travel.guide.mapper.MmContentMapper;
import com.rural.travel.guide.vo.ContentProduceVO;
import com.rural.travel.security.SecurityUtils;
import com.rural.travel.service.UserAiConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * S2 多模态数字内容生成引擎。
 *
 * <p>一个实体 × 四类模态(文案/语音稿/图像/视频脚本) × 三种风格 × 两种方言 × 三档深度，
 * 由引擎一次批量产出，实现"低成本文旅内容量产"；内容完备度 g_i 回写知识图谱实体，
 * 成为式(1) 的一项输入，形成"内容越多 → 越容易被推荐 → 越值得再生产"的正循环。</p>
 *
 * <p>大模型调用使用操作者自己的密钥，失败或未配置时自动降级本地模板，保证县域零预算也能跑。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultimodalContentService {

    /** 四类模态 */
    private static final List<String> ALL_TYPES = List.of("TEXT", "AUDIO", "IMAGE", "VIDEO");
    /** 三种风格 */
    private static final List<String> ALL_STYLES = List.of("清新", "活泼", "文艺");
    /** 两种语言形态 */
    private static final List<String> ALL_DIALECTS = List.of("普通话", "方言");

    private final KgService kgService;
    private final MmContentMapper contentMapper;
    private final AiClient aiClient;
    private final UserAiConfigService userAiConfigService;

    /** 批量生产：已存在同规格内容则跳过，保证幂等 */
    public ContentProduceVO produce(Long entityId, ContentBatchDTO dto, Long operatorId) {
        KgEntity entity = kgService.get(entityId);
        if (entity == null) throw new IllegalArgumentException("知识图谱实体不存在: " + entityId);

        double before = entity.getContentScore() == null ? 0 : entity.getContentScore();

        List<String> types = emptyToAll(dto.getContentTypes(), ALL_TYPES);
        List<String> styles = emptyToAll(dto.getStyles(), ALL_STYLES);
        List<String> dialects = emptyToAll(dto.getDialects(), ALL_DIALECTS);
        boolean useAi = dto.getUseAi() == null || dto.getUseAi();

        UserAiConfigService.AiKey key = useAi
                ? userAiConfigService.keyOf(operatorId != null ? operatorId : SecurityUtils.currentUserIdOrNull())
                : null;

        int produced = 0, skipped = 0;
        List<Long> ids = new ArrayList<>();
        String engineUsed = "本地模板";

        for (String type : types) {
            for (String style : styles) {
                if ("IMAGE".equals(type) && !"清新".equals(style)) {
                    // 图像描述按风格差异不大，只出一份，避免无意义堆积
                    skipped++;
                    continue;
                }
                for (String dialect : dialects) {
                    int depth = 2;   // 批量生产默认标准档，简约/深度档由位置触发按需生成
                    boolean exists = contentMapper.selectCount(new LambdaQueryWrapper<MmContent>()
                            .eq(MmContent::getEntityId, entityId)
                            .eq(MmContent::getContentType, type)
                            .eq(MmContent::getStyle, style)
                            .eq(MmContent::getDialect, dialect)
                            .eq(MmContent::getDepthLevel, depth)) > 0;
                    if (exists) {
                        skipped++;
                        continue;
                    }
                    String body = generate(key, entity, type, style, dialect, depth);
                    MmContent c = new MmContent();
                    c.setEntityId(entityId);
                    c.setContentType(type);
                    c.setStyle(style);
                    c.setDialect(dialect);
                    c.setDepthLevel(depth);
                    c.setTitle(titleOf(entity, type, style, dialect));
                    c.setBody(body);
                    c.setMediaUrl("IMAGE".equals(type) ? entity.getCoverImage() : null);
                    c.setDurationSec(estimateSeconds(body));
                    c.setEngine(key == null ? "本地模板" : "AI增强");
                    c.setAuditStatus(1);
                    c.setCreatedBy(operatorId);
                    contentMapper.insert(c);
                    ids.add(c.getId());
                    produced++;
                    if (key != null) engineUsed = "AI增强";
                }
            }
        }

        double after = kgService.recalcContentScore(entityId);

        ContentProduceVO vo = new ContentProduceVO();
        vo.setEntityId(entityId);
        vo.setEntityName(entity.getName());
        vo.setProduced(produced);
        vo.setSkipped(skipped);
        vo.setEngine(engineUsed);
        vo.setContentScoreBefore(before);
        vo.setContentScoreAfter(after);
        vo.setContentIds(ids);
        return vo;
    }

    /** 大模型优先，失败降级本地模板 */
    private String generate(UserAiConfigService.AiKey key, KgEntity e, String type,
                            String style, String dialect, int depth) {
        if (key != null) {
            String prompt = buildPrompt(e, type, style, dialect, depth);
            String content = aiClient.chatWith(key.apiKey(), key.baseUrl(), key.model(),
                    "你是资深县域文旅内容编辑。你写的稿子要能被直接配音播出或排版发布，"
                            + "语言自然、有画面感、不堆砌辞藻，不写任何解释性开场白和结尾备注。", prompt);
            if (StringUtils.hasText(content)) {
                return "方言".equals(dialect) ? toDialect(content.trim()) : content.trim();
            }
            log.warn("大模型生成失败，降级本地模板: entity={} type={}", e.getId(), type);
        }
        return template(e, type, style, dialect, depth);
    }

    private String buildPrompt(KgEntity e, String type, String style, String dialect, int depth) {
        String typeName = switch (type) {
            case "TEXT" -> "图文推报文案";
            case "AUDIO" -> "语音导览讲解稿";
            case "IMAGE" -> "宣传画面描述（含构图、光线、主体、色调，供美术或文生图使用）";
            case "VIDEO" -> "60 秒短视频分镜脚本（含时间轴、镜头、画面、旁白、字幕）";
            default -> "宣传文案";
        };
        int words = switch (depth) {
            case 1 -> 80;
            case 3 -> 420;
            default -> 180;
        };
        return "请为县域文旅资源「" + e.getName() + "」创作一份" + typeName + "。\n"
                + "资源类型:" + typeName(e.getEntityType()) + "\n"
                + "简介:" + nvl(e.getSummary()) + "\n"
                + "详情:" + nvl(e.getDetail()) + "\n"
                + "标签:" + String.join("、", KgService.splitTags(e.getTags())) + "\n"
                + "建议游览时长:" + nvl(e.getVisitMinutes()) + "分钟\n"
                + "风格要求:" + style + "\n"
                + "语言形态:" + ("方言".equals(dialect) ? "用吴语片区的地方话口吻改写，保留少量方言词，仍要让外地游客听得懂" : "标准普通话") + "\n"
                + "深度档:" + depth + "档（1简约 2标准 3深度）\n"
                + "篇幅:控制在 " + words + " 字左右。\n"
                + "直接输出成稿正文，不要标题，不要 Markdown 语法，不要任何说明。";
    }

    // ---------------- 本地模板引擎（零预算兜底） ----------------

    private String template(KgEntity e, String type, String style, String dialect, int depth) {
        String name = e.getName();
        String summary = nvl(e.getSummary());
        String first = summary.isEmpty() ? name : summary.split("[。！？]")[0];
        String tags = KgService.splitTags(e.getTags()).stream().map(t -> "#" + t)
                .reduce("", (a, b) -> a + " " + b).trim();
        int minutes = e.getVisitMinutes() == null ? 30 : e.getVisitMinutes();
        String open = nvl(e.getOpenTime()).isEmpty() ? "全天开放" : e.getOpenTime();
        String opening = switch (style) {
            case "活泼" -> "来咯来咯！" + name + "安排上！";
            case "文艺" -> "山有脉，水有源，" + name + "有自己的慢时光。";
            default -> "如果你也想找个地方慢下来，" + name + "值得留半天。";
        };
        String body = switch (type) {
            case "TEXT" -> opening + "\n\n" + summary + (depth >= 3 ? "\n\n" + detailOf(e) : "")
                    + "\n\n建议停留 " + minutes + " 分钟，开放时间 " + open + "。\n" + tags + " #县域文旅 #周末去哪儿";
            case "AUDIO" -> {
                String base = "各位游客，您现在来到的是" + name + "。" + first + "。"
                        + (depth >= 3 ? detailOf(e) : "")
                        + "建议您留步 " + minutes + " 分钟，慢慢看。";
                yield depth == 1 ? name + "。" + first + "。" : base;
            }
            case "IMAGE" -> "【画面主体】" + name + "，" + first + "\n"
                    + "【构图】三分法，前景留白，主体偏右下\n"
                    + "【光线】" + (depth >= 3 ? "清晨侧逆光，暖调，长阴影" : "午后柔光，通透") + "\n"
                    + "【色调】" + switch (style) {
                        case "活泼" -> "高饱和，青绿+暖橙撞色";
                        case "文艺" -> "低饱和，青灰+米白，胶片颗粒";
                        default -> "自然还原，草木绿+天空蓝";
                    } + "\n"
                    + "【画面元素】" + nvl(e.getTags()) + "\n"
                    + "【文生图提示词】" + promptOf(e, style);
            case "VIDEO" -> "【0-5s 开场】全景缓推，字幕:" + name + "\n"
                    + "【5-20s 看点一】" + first + "，特写+慢动作\n"
                    + "【20-40s 看点二】" + (depth >= 3 ? detailOf(e) : "游人互动镜头，笑容与手艺特写") + "\n"
                    + "【40-55s 看点三】" + switch (style) {
                        case "活泼" -> "快剪踩点，展示吃喝玩乐";
                        case "文艺" -> "空镜+环境音，情绪留白";
                        default -> "日落暖光收尾，节奏放缓";
                    } + "\n"
                    + "【55-60s 结尾】定格定位卡片，口播:来" + name + "，把节奏放慢。\n"
                    + "BGM 建议:" + (depth >= 3 ? "民乐+自然白噪音" : "轻快民谣") + " | 话题:" + tags;
            default -> summary;
        };
        return "方言".equals(dialect) ? toDialect(body) : body;
    }

    private String detailOf(KgEntity e) {
        String d = nvl(e.getDetail());
        return d.isEmpty() ? nvl(e.getSummary()) : d;
    }

    /** 文生图提示词：给后续图像生成直接食用 */
    private String promptOf(KgEntity e, String style) {
        String mood = switch (style) {
            case "活泼" -> "vibrant colors, lively, sunny";
            case "文艺" -> "muted film tones, poetic, soft light";
            default -> "natural colors, fresh, bright";
        };
        return String.join(", ",
                "Chinese county cultural tourism",
                nvl(e.getName()),
                String.join(" ", KgService.splitTags(e.getTags())),
                "photorealistic, high detail, " + mood);
    }

    /**
     * 方言改写：基于规则的地方话口吻转换。
     * 只做轻量词汇替换与语气词点缀，保证外地游客仍听得懂。
     */
    private String toDialect(String text) {
        if (!StringUtils.hasText(text)) return text;
        String s = text;
        String[][] rules = {
                {"这里", "格搭"}, {"那里", "伊搭"}, {"现在", "这辰光"},
                {"什么", "啥个"}, {"怎么", "哪能"}, {"非常", "交关"},
                {"很好", "蛮好"}, {"特别", "邪气"}, {"一点", "一眼眼"},
                {"小朋友", "小囡"}, {"时候", "辰光"}, {"一起", "一道"},
                {"知道", "晓得"}, {"看看", "看看看"}, {"慢慢", "慢慢叫"},
        };
        for (String[] r : rules) {
            s = s.replace(r[0], r[1]);
        }
        // 句末语气词点缀，避免通篇替换后仍像书面语
        s = s.replace("。", " 呀。").replace(" 呀。\n", "。\n");
        return s;
    }

    private String titleOf(KgEntity e, String type, String style, String dialect) {
        String t = switch (type) {
            case "TEXT" -> "图文文案";
            case "AUDIO" -> "语音讲解稿";
            case "IMAGE" -> "宣传画面描述";
            case "VIDEO" -> "短视频脚本";
            default -> "内容";
        };
        return e.getName() + " · " + t + " · " + style + " · " + dialect;
    }

    /** 中文播报约 240 字/分钟 */
    private int estimateSeconds(String body) {
        if (!StringUtils.hasText(body)) return 0;
        return (int) Math.max(5, Math.round(body.length() / 4.0));
    }

    private List<String> emptyToAll(List<String> given, List<String> all) {
        if (given == null || given.isEmpty()) return all;
        return given.stream().filter(StringUtils::hasText).toList();
    }

    private String nvl(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    public static String typeName(String code) {
        if (code == null) return "文旅资源";
        return switch (code) {
            case "SCENIC" -> "景点";
            case "HERITAGE" -> "非物质文化遗产";
            case "FOOD" -> "特色美食";
            case "VILLAGE" -> "传统村落";
            case "PERSON" -> "传承人";
            case "CRAFT" -> "传统技艺";
            default -> "文旅资源";
        };
    }

    // ---------------- 检索（S5 取内容用） ----------------

    /**
     * 按实体、模态、深度、方言取最合适的一份内容。
     * 逐级降级：精确匹配 → 同方言标准档 → 普通话同档 → 普通话标准档。
     */
    public MmContent pick(Long entityId, String contentType, Integer depthLevel, String dialect) {
        List<MmContent> all = contentMapper.selectList(new LambdaQueryWrapper<MmContent>()
                .eq(MmContent::getEntityId, entityId)
                .eq(StringUtils.hasText(contentType), MmContent::getContentType, contentType)
                .eq(MmContent::getAuditStatus, 1));
        if (all.isEmpty()) return null;
        String wantDialect = StringUtils.hasText(dialect) ? dialect : "普通话";
        int wantDepth = depthLevel == null ? 2 : depthLevel;

        return all.stream()
                .filter(c -> wantDialect.equals(c.getDialect()) && wantDepth == c.getDepthLevel()).findFirst()
                .orElseGet(() -> all.stream()
                        .filter(c -> wantDialect.equals(c.getDialect()) && c.getDepthLevel() == 2).findFirst()
                        .orElseGet(() -> all.stream()
                                .filter(c -> "普通话".equals(c.getDialect()) && c.getDepthLevel() == wantDepth).findFirst()
                                .orElseGet(() -> all.stream()
                                        .filter(c -> "普通话".equals(c.getDialect()) && c.getDepthLevel() == 2).findFirst()
                                        .orElse(all.get(0)))));
    }

    public List<MmContent> ofEntity(Long entityId) {
        return contentMapper.selectList(new LambdaQueryWrapper<MmContent>()
                .eq(MmContent::getEntityId, entityId)
                .orderByAsc(MmContent::getContentType)
                .orderByAsc(MmContent::getDepthLevel));
    }

    /** 内容曝光/播放埋点，供效果指标统计 */
    public void bumpExposure(Long contentId) {
        MmContent c = contentMapper.selectById(contentId);
        if (c == null) return;
        c.setExposure((c.getExposure() == null ? 0 : c.getExposure()) + 1);
        c.setPlayCount((c.getPlayCount() == null ? 0 : c.getPlayCount()) + 1);
        contentMapper.updateById(c);
    }

    public void bumpFinish(Long contentId) {
        MmContent c = contentMapper.selectById(contentId);
        if (c == null) return;
        c.setFinishCount((c.getFinishCount() == null ? 0 : c.getFinishCount()) + 1);
        contentMapper.updateById(c);
    }
}
