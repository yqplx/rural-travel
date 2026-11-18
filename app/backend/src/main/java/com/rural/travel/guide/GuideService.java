package com.rural.travel.guide;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rural.travel.ai.AiClient;
import com.rural.travel.guide.dto.GuideAskDTO;
import com.rural.travel.guide.dto.GuideTriggerDTO;
import com.rural.travel.guide.entity.GuideSession;
import com.rural.travel.guide.entity.KgEntity;
import com.rural.travel.guide.entity.MmContent;
import com.rural.travel.guide.entity.VisitorProfile;
import com.rural.travel.guide.mapper.GuideSessionMapper;
import com.rural.travel.guide.vo.GuideAskVO;
import com.rural.travel.guide.vo.GuideTriggerVO;
import com.rural.travel.service.KnowledgeService;
import com.rural.travel.service.UserAiConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * S5 位置触发自适应讲解与语音问答。
 *
 * <p>游客进入实体触发半径后自动判定讲解深度档位（式(3)），
 * 按档位与方言取用对应内容；内容缺失时降级为基础介绍，并在返回中给出降级说明。</p>
 *
 * <pre>
 * 式(3) d_u = ⌊θ1·k_u + θ2·r_u + θ3·q_u + 1⌋，且 d_u ≤ 3
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuideService {

    private final KgService kgService;
    private final VisitorProfileService profileService;
    private final MultimodalContentService contentService;
    private final GuideSessionMapper sessionMapper;
    private final KnowledgeService knowledgeService;
    private final UserAiConfigService userAiConfigService;
    private final AiClient aiClient;

    /** 位置触发自适应讲解 */
    public GuideTriggerVO trigger(Long userId, GuideTriggerDTO dto) {
        KgEntity entity = resolveEntity(dto);
        if (entity == null) {
            GuideTriggerVO empty = new GuideTriggerVO();
            empty.setTriggered(false);
            empty.setDegradeNote("当前位置附近没有已入库的文旅资源，可先在管理端补充知识图谱实体。");
            return empty;
        }

        VisitorProfile profile = userId == null ? null : profileService.getOrCreate(userId);
        // 式(3) 三个自变量：k_u 知识背景水平 / r_u 互动参与度 / q_u 同行人员构成因子，均 ∈ [0,3]
        double k = profile == null || profile.getDepthLevel() == null ? 2 : profile.getDepthLevel();
        int recommended = entity.getVisitMinutes() == null ? 30 : entity.getVisitMinutes();
        double r = GuideFormulas.interactionDegree(
                dto.getDwellSeconds() == null ? 0 : dto.getDwellSeconds(),
                recommended,
                dto.getAskCount() == null ? 0 : dto.getAskCount());
        double q = profile == null ? 2.0 : GuideFormulas.groupFactor(profile.getGroupType());
        int depth = GuideFormulas.guideDepth(k, r, q);

        String dialect = StringUtils.hasText(dto.getDialect())
                ? dto.getDialect()
                : (profile == null ? "普通话" : nvl(profile.getPreferDialect(), "普通话"));

        MmContent content = contentService.pick(entity.getId(), "AUDIO", depth, dialect);
        if (content == null) content = contentService.pick(entity.getId(), "TEXT", depth, dialect);

        GuideSession session = new GuideSession();
        session.setUserId(userId);
        session.setRouteId(dto.getRouteId());
        session.setEntityId(entity.getId());
        session.setContentId(content == null ? null : content.getId());
        session.setTriggerLng(dto.getLng());
        session.setTriggerLat(dto.getLat());
        session.setDepthLevel(depth);
        session.setDepthDetail(buildDepthDetail(k, r, q, depth));
        session.setDwellSeconds(dto.getDwellSeconds() == null ? 0 : dto.getDwellSeconds());
        session.setAskCount(dto.getAskCount() == null ? 0 : dto.getAskCount());
        double distance = distanceTo(entity, dto.getLng(), dto.getLat());
        session.setDistanceM(distance < 0 ? null : (int) Math.round(distance));
        sessionMapper.insert(session);

        if (content != null) contentService.bumpExposure(content.getId());

        GuideTriggerVO vo = new GuideTriggerVO();
        vo.setSessionId(session.getId());
        vo.setEntityId(entity.getId());
        vo.setEntityName(entity.getName());
        vo.setEntityType(entity.getEntityType());
        vo.setCoverImage(entity.getCoverImage());
        vo.setLng(entity.getLng());
        vo.setLat(entity.getLat());
        vo.setDistanceM(distance < 0 ? null : (int) Math.round(distance));
        vo.setTriggered(distance < 0 || distance <= GuideFormulas.TRIGGER_RADIUS_M);
        vo.setDepthLevel(depth);
        vo.setDepthName(VisitorProfileService.depthName(depth));
        vo.setDepthDetail(session.getDepthDetail());
        vo.setKValue(k);
        vo.setRValue(Math.round(r * 100.0) / 100.0);
        vo.setQValue(q);
        vo.setContentId(content == null ? null : content.getId());
        vo.setContentType(content == null ? null : content.getContentType());
        vo.setTitle(content == null ? entity.getName() : content.getTitle());
        vo.setBody(content == null ? fallbackBody(entity) : content.getBody());
        vo.setDialect(dialect);
        vo.setStyle(content == null ? "标准" : content.getStyle());
        vo.setMediaUrl(content == null ? null : content.getMediaUrl());
        vo.setDurationSec(content == null ? estimateSeconds(vo.getBody()) : content.getDurationSec());
        vo.setEngine(content == null ? "本地模板" : content.getEngine());
        vo.setDegradeNote(content == null
                ? "该点位还没有生成" + depth + "档语音讲解，当前展示基础介绍；可在内容工作台一键补齐。"
                : null);
        return vo;
    }

    /** 语音问答：图谱 + 知识库检索增强，未配密钥时降级本地拼装 */
    public GuideAskVO ask(Long userId, GuideAskDTO dto) {
        KgEntity entity = dto.getEntityId() == null ? null : kgService.get(dto.getEntityId());
        String question = nvl(dto.getQuestion(), "");
        GuideAskVO vo = new GuideAskVO();
        vo.setQuestion(question);

        List<String> references = new ArrayList<>();
        StringBuilder ctx = new StringBuilder();
        if (entity != null) {
            ctx.append("资源名称:").append(entity.getName())
                    .append("\n类型:").append(MultimodalContentService.typeName(entity.getEntityType()))
                    .append("\n简介:").append(nvl(entity.getSummary()))
                    .append("\n详情:").append(nvl(entity.getDetail()))
                    .append("\n标签:").append(String.join("、", KgService.splitTags(entity.getTags())))
                    .append("\n");
            vo.setEntityId(entity.getId());
            vo.setEntityName(entity.getName());
        }
        for (KnowledgeService.ScoredEntry se : knowledgeService.search(question, 3)) {
            if (se.score() <= 0) continue;
            ctx.append("知识库:").append(se.entry().getQuestion()).append(" -> ")
                    .append(se.entry().getAnswer()).append("\n");
            references.add(se.entry().getQuestion());
        }

        UserAiConfigService.AiKey key = userAiConfigService.keyOf(userId);
        String answer = null;
        if (key != null && StringUtils.hasText(question)) {
            String dialect = StringUtils.hasText(dto.getDialect()) ? dto.getDialect() : "普通话";
            answer = aiClient.chatWith(key.apiKey(), key.baseUrl(), key.model(),
                    "你是县域文旅智能导览助手。只依据给定资料回答，资料没有的内容就说“这个我这边没有确切资料，建议到游客中心问问”。"
                            + "回答控制在 120 字以内，口语化，适合语音播报。"
                            + ("方言".equals(dialect) ? "请用带地方口吻但外地人能听懂的说法。" : "请用标准普通话。"),
                    "资料:\n" + ctx + "\n游客问题:" + question);
        }
        if (StringUtils.hasText(answer)) {
            vo.setAnswer(answer.trim());
            vo.setEngine("AI增强");
        } else {
            vo.setAnswer(localAnswer(question, entity, ctx.toString()));
            vo.setEngine(references.isEmpty() ? "本地模板" : "知识库");
        }
        vo.setReferences(references);
        vo.setFollowUps(followUps(entity));

        // 提问会提升式(3) 的 q_u，使后续讲解自动加深
        if (dto.getSessionId() != null) {
            GuideSession s = sessionMapper.selectById(dto.getSessionId());
            if (s != null) {
                s.setAskCount((s.getAskCount() == null ? 0 : s.getAskCount()) + 1);
                sessionMapper.updateById(s);
            }
        }
        return vo;
    }

    /** 更新会话停留时长：前端定时回传，用于式(3) 的 r_u */
    public void touch(Long sessionId, Integer dwellSeconds) {
        if (sessionId == null || dwellSeconds == null) return;
        GuideSession s = sessionMapper.selectById(sessionId);
        if (s == null) return;
        s.setDwellSeconds(dwellSeconds);
        sessionMapper.updateById(s);
        if (s.getContentId() != null && dwellSeconds >= 60) {
            contentService.bumpFinish(s.getContentId());
        }
    }

    public List<GuideSession> mySessions(Long userId, int limit) {
        return sessionMapper.selectList(new LambdaQueryWrapper<GuideSession>()
                .eq(GuideSession::getUserId, userId)
                .orderByDesc(GuideSession::getCreatedAt)
                .last("LIMIT " + Math.max(1, Math.min(100, limit))));
    }

    // ---------------- 内部 ----------------

    private KgEntity resolveEntity(GuideTriggerDTO dto) {
        if (dto.getEntityId() != null) return kgService.get(dto.getEntityId());
        if (dto.getLng() == null || dto.getLat() == null) return null;
        List<KgEntity> all = kgService.list(null, null).stream()
                .filter(e -> e.getLng() != null && e.getLat() != null).toList();
        KgEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (KgEntity e : all) {
            double d = GuideFormulas.distanceMeters(dto.getLng(), dto.getLat(), e.getLng(), e.getLat());
            if (d < best) {
                best = d;
                nearest = e;
            }
        }
        // 超出 5 倍触发半径就认为不在任何点位附近
        return (nearest != null && best <= GuideFormulas.TRIGGER_RADIUS_M * 5) ? nearest : null;
    }

    private double distanceTo(KgEntity e, Double lng, Double lat) {
        if (lng == null || lat == null || e.getLng() == null || e.getLat() == null) return -1;
        return GuideFormulas.distanceMeters(lng, lat, e.getLng(), e.getLat());
    }

    private String buildDepthDetail(double k, double r, double q, int depth) {
        return String.format(
                "式(3) d=⌊θ1·k_u + θ2·r_u + θ3·q_u + 1⌋ = ⌊%.2f×%.2f(知识背景) + %.2f×%.2f(互动参与) + %.2f×%.2f(同行构成) + 1⌋ = %d（上限 3）",
                GuideFormulas.THETA1, k, GuideFormulas.THETA2, r, GuideFormulas.THETA3, q, depth);
    }

    private String fallbackBody(KgEntity e) {
        return "您现在来到的是" + e.getName() + "。" + nvl(e.getSummary()) + "建议停留 "
                + (e.getVisitMinutes() == null ? 30 : e.getVisitMinutes()) + " 分钟。";
    }

    private int estimateSeconds(String body) {
        return body == null ? 0 : Math.max(5, body.length() / 4);
    }

    private String localAnswer(String question, KgEntity entity, String ctx) {
        if (entity != null && StringUtils.hasText(question)) {
            if (question.contains("多久") || question.contains("时间") || question.contains("长")) {
                return entity.getName() + "建议安排 "
                        + (entity.getVisitMinutes() == null ? 30 : entity.getVisitMinutes())
                        + " 分钟，开放时间" + (StringUtils.hasText(entity.getOpenTime()) ? entity.getOpenTime() : "以现场公告为准") + "。";
            }
            if (question.contains("在哪") || question.contains("位置") || question.contains("怎么走")) {
                return entity.getName() + "就在当前导览点位上，跟着路线走即可到达；需要的话我可以让讲解再详细一些。";
            }
            if (StringUtils.hasText(entity.getSummary())) {
                return entity.getSummary();
            }
        }
        if (StringUtils.hasText(ctx)) return ctx.trim();
        return "这个我这边暂时没有确切资料，建议你到游客中心问问，或者换个说法再问我一次。";
    }

    private List<String> followUps(KgEntity entity) {
        List<String> list = new ArrayList<>(List.of("这里有什么典故？", "附近还有什么值得去？", "大概要逛多久？"));
        if (entity != null && "FOOD".equals(entity.getEntityType())) {
            list.add(0, "哪家店最正宗？");
        }
        if (entity != null && "HERITAGE".equals(entity.getEntityType())) {
            list.add(0, "这项技艺怎么传承下来的？");
        }
        return list.stream().limit(3).collect(Collectors.toList());
    }

    private String nvl(String v, String def) {
        return StringUtils.hasText(v) ? v : def;
    }

    private String nvl(String v) {
        return v == null ? "" : v;
    }
}
