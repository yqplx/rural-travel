package com.rural.travel.guide;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rural.travel.guide.entity.*;
import com.rural.travel.guide.mapper.*;
import com.rural.travel.guide.vo.PolicyMetricsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 政策契合度指标看板。
 *
 * <p>对应三大政策方向：</p>
 * <ul>
 *   <li>创新数字内容消费 —— 多模态内容产量、AI 生成占比、模态覆盖度、方言内容数</li>
 *   <li>智慧文旅发展 —— 导览触发次数、路线数、游客满意度</li>
 *   <li>乡村振兴数智赋能 —— 覆盖县域数、资源实体数、非遗与美食数字化数量</li>
 * </ul>
 *
 * <p>提升幅度三个数值来自说明书实施例的实测口径，作为对照基准展示。</p>
 */
@Service
@RequiredArgsConstructor
public class PolicyMetricsService {

    private final KgCountyMapper countyMapper;
    private final KgEntityMapper entityMapper;
    private final MmContentMapper contentMapper;
    private final GuideRouteMapper routeMapper;
    private final GuideSessionMapper sessionMapper;
    private final GuideFeedbackMapper feedbackMapper;
    private final FeedbackService feedbackService;

    /** 实施例口径的对照基准 */
    private static final double BASE_CONTENT_EFFICIENCY = 0.40;
    private static final double BASE_CONVERSION = 0.35;
    private static final double BASE_SATISFACTION = 0.80;

    public PolicyMetricsVO metrics() {
        PolicyMetricsVO vo = new PolicyMetricsVO();

        // ---------- 创新数字内容消费 ----------
        List<MmContent> contents = contentMapper.selectList(null);
        vo.setContentTotal(contents.size());
        long aiCount = contents.stream().filter(c -> "AI增强".equals(c.getEngine())).count();
        vo.setContentAiGenerated((int) aiCount);
        vo.setAiRatio(contents.isEmpty() ? 0 : round(aiCount * 1.0 / contents.size()));
        long modalities = contents.stream().map(MmContent::getContentType).distinct().count();
        vo.setModalityCovered((int) modalities);

        List<PolicyMetricsVO.NameValue> byModality = new ArrayList<>();
        for (String t : List.of("TEXT", "AUDIO", "IMAGE", "VIDEO")) {
            byModality.add(new PolicyMetricsVO.NameValue(typeLabel(t),
                    contents.stream().filter(c -> t.equals(c.getContentType())).count()));
        }
        vo.setByModality(byModality);

        List<PolicyMetricsVO.NameValue> byStyle = new ArrayList<>();
        for (String s : List.of("清新", "活泼", "文艺")) {
            byStyle.add(new PolicyMetricsVO.NameValue(s,
                    contents.stream().filter(c -> s.equals(c.getStyle())).count()));
        }
        vo.setByStyle(byStyle);
        vo.setDialectCount((int) contents.stream().filter(c -> "方言".equals(c.getDialect())).count());

        // ---------- 实体与内容覆盖 ----------
        List<KgEntity> entities = entityMapper.selectList(new LambdaQueryWrapper<KgEntity>().eq(KgEntity::getStatus, 1));
        vo.setEntityTotal(entities.size());
        vo.setHeritageCount((int) entities.stream().filter(e -> "HERITAGE".equals(e.getEntityType())).count());
        vo.setFoodCount((int) entities.stream().filter(e -> "FOOD".equals(e.getEntityType())).count());
        long withContent = entities.stream().filter(e -> e.getContentScore() != null && e.getContentScore() > 0).count();
        vo.setContentCoverage(entities.isEmpty() ? 0 : round(withContent * 1.0 / entities.size()));
        vo.setCountyCount(countyMapper.selectCount(
                new LambdaQueryWrapper<KgCounty>().eq(KgCounty::getStatus, 1)).intValue());

        // ---------- 智慧文旅发展 ----------
        vo.setGuideSessions(sessionMapper.selectCount(null).intValue());
        List<GuideRoute> routes = routeMapper.selectList(null);
        vo.setRouteCount(routes.size());
        vo.setAvgRouteNodes(routes.isEmpty() ? 0
                : round(routes.stream().mapToInt(r -> r.getItemCount() == null ? 0 : r.getItemCount())
                .average().orElse(0)));

        List<GuideFeedback> feedbacks = feedbackMapper.selectList(null);
        vo.setFeedbackCount(feedbacks.size());
        vo.setSatisfaction(feedbacks.isEmpty() ? 0
                : round(feedbacks.stream().mapToInt(f -> f.getScore() == null ? 0 : f.getScore())
                .average().orElse(0) / 5.0));

        // ---------- 乡村振兴数智赋能 ----------
        vo.setContentEfficiencyGain(BASE_CONTENT_EFFICIENCY);
        vo.setConversionGain(BASE_CONVERSION);
        vo.setSatisfactionGain(BASE_SATISFACTION);

        vo.setEmaSamples(feedbackService.emaSamples(6));
        return vo;
    }

    private static String typeLabel(String t) {
        return switch (t) {
            case "TEXT" -> "图文文案";
            case "AUDIO" -> "语音讲解";
            case "IMAGE" -> "宣传画面";
            case "VIDEO" -> "视频脚本";
            default -> t;
        };
    }

    private double round(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
