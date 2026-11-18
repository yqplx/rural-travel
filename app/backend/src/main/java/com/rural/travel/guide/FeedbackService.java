package com.rural.travel.guide;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rural.travel.guide.dto.GuideFeedbackDTO;
import com.rural.travel.guide.entity.GuideFeedback;
import com.rural.travel.guide.entity.KgEntity;
import com.rural.travel.guide.entity.MmContent;
import com.rural.travel.guide.mapper.GuideFeedbackMapper;
import com.rural.travel.guide.mapper.KgEntityMapper;
import com.rural.travel.guide.mapper.MmContentMapper;
import com.rural.travel.guide.vo.PolicyMetricsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * S6 反馈闭环：把游客评分回灌到内容满意度与实体热度，形成自优化闭环。
 *
 * <pre>
 * 式(4) h_i^(t+1) = (1 − η)·h_i^(t) + η·f_i^(t)，η = 0.20
 * </pre>
 *
 * <p>同一份内容的新反馈按指数平滑更新，既响应新数据，又不会被单次极端评分带偏；
 * 更新后的 h_i 直接进入式(1) 的 β·h_i，使下一轮路线规划自动偏向游客真正认可的点位。</p>
 */
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final GuideFeedbackMapper feedbackMapper;
    private final MmContentMapper contentMapper;
    private final KgEntityMapper entityMapper;

    /** 提交反馈并触发式(4) 更新 */
    @Transactional
    public Map<String, Object> submit(Long userId, GuideFeedbackDTO dto) {
        int score = dto.getScore() == null ? 5 : Math.max(1, Math.min(5, dto.getScore()));
        MmContent content = dto.getContentId() == null ? null : contentMapper.selectById(dto.getContentId());
        Long entityId = dto.getEntityId();
        if (entityId == null && content != null) entityId = content.getEntityId();
        if (entityId == null) throw new IllegalArgumentException("缺少 entityId");

        // 一人一份内容一条反馈：重复提交按更新处理
        GuideFeedback fb = null;
        if (content != null) {
            fb = feedbackMapper.selectOne(new LambdaQueryWrapper<GuideFeedback>()
                    .eq(GuideFeedback::getUserId, userId)
                    .eq(GuideFeedback::getContentId, content.getId()));
        }
        if (fb == null) {
            fb = new GuideFeedback();
            fb.setUserId(userId);
            fb.setEntityId(entityId);
            fb.setContentId(content == null ? null : content.getId());
            fb.setScore(score);
            fb.setComment(dto.getComment());
            feedbackMapper.insert(fb);
        } else {
            fb.setScore(score);
            fb.setComment(dto.getComment());
            feedbackMapper.updateById(fb);
        }

        double f = score / 5.0;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("feedbackId", fb.getId());
        result.put("score", score);
        result.put("normalized", round(f));

        // ---- 式(4)：内容侧满意度 EMA ----
        if (content != null) {
            double before = content.getScoreEma() == null ? 0 : content.getScoreEma();
            double after = GuideFormulas.nextEma(before, f);
            content.setScoreEma(after);
            content.setScoreSum((content.getScoreSum() == null ? 0 : content.getScoreSum()) + score);
            content.setScoreCount((content.getScoreCount() == null ? 0 : content.getScoreCount()) + 1);
            contentMapper.updateById(content);
            result.put("contentId", content.getId());
            result.put("contentEmaBefore", round(before));
            result.put("contentEmaAfter", round(after));
        }

        // ---- 式(4)：实体侧热度 EMA ----
        KgEntity entity = entityMapper.selectById(entityId);
        if (entity != null) {
            double before = entity.getHeat() == null ? 0 : entity.getHeat();
            double after = GuideFormulas.nextEma(before, f);
            entity.setHeat(after);
            entityMapper.updateById(entity);
            result.put("entityId", entity.getId());
            result.put("entityName", entity.getName());
            result.put("heatBefore", round(before));
            result.put("eta", GuideFormulas.ETA);
            result.put("heatAfter", round(after));
            result.put("formula", "h(t+1) = (1 − η)·h(t) + η·f(t)");
        }
        return result;
    }

    public List<GuideFeedback> myFeedback(Long userId) {
        return feedbackMapper.selectList(new LambdaQueryWrapper<GuideFeedback>()
                .eq(GuideFeedback::getUserId, userId)
                .orderByDesc(GuideFeedback::getCreatedAt));
    }

    /** 式(4) 演示样本：取热度最高的若干实体，展示一次反馈前后的变化 */
    public List<PolicyMetricsVO.EmaSample> emaSamples(int limit) {
        List<KgEntity> top = entityMapper.selectList(new LambdaQueryWrapper<KgEntity>()
                .eq(KgEntity::getStatus, 1)
                .orderByDesc(KgEntity::getHeat)
                .last("LIMIT " + Math.max(1, Math.min(20, limit))));
        List<PolicyMetricsVO.EmaSample> samples = new ArrayList<>();
        for (KgEntity e : top) {
            double before = e.getHeat() == null ? 0 : e.getHeat();
            double f = 0.9;   // 演示：一次 4.5 星反馈
            samples.add(new PolicyMetricsVO.EmaSample(e.getName(), round(before), round(f),
                    round(GuideFormulas.nextEma(before, f))));
        }
        return samples;
    }

    private double round(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
