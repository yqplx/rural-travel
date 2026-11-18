package com.rural.travel.ai;

import com.rural.travel.entity.KnowledgeEntry;
import com.rural.travel.entity.ScenicSpot;
import com.rural.travel.exception.BizException;
import com.rural.travel.security.SecurityUtils;
import com.rural.travel.service.KnowledgeService;
import com.rural.travel.service.ScenicSpotService;
import com.rural.travel.service.UserAiConfigService;
import com.rural.travel.vo.ChatVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/** 智能问答:知识库检索打分 + 大模型增强,游客免登录可用 */
@Service
public class AiChatService {

    private final KnowledgeService knowledgeService;
    private final ScenicSpotService scenicSpotService;
    private final AiClient aiClient;
    private final UserAiConfigService userAiConfigService;

    @Value("${rural.ai.max-knowledge-context:5}")
    private int maxKnowledgeContext;

    public AiChatService(KnowledgeService knowledgeService, ScenicSpotService scenicSpotService,
                         AiClient aiClient, UserAiConfigService userAiConfigService) {
        this.knowledgeService = knowledgeService;
        this.scenicSpotService = scenicSpotService;
        this.aiClient = aiClient;
        this.userAiConfigService = userAiConfigService;
    }

    public ChatVO chat(String message) {
        if (!StringUtils.hasText(message)) throw new BizException("请输入想问的问题");
        if (message.length() > 500) throw new BizException("问题太长了,精简一点再问我吧");
        message = message.trim();

        List<KnowledgeService.ScoredEntry> top = knowledgeService.search(message, maxKnowledgeContext);
        String matchedQuestion = top.isEmpty() ? null : top.get(0).entry().getQuestion();

        // 大模型增强:使用该用户自己的密钥;游客或未配置的用户自动降级本地引擎
        Long userId = SecurityUtils.currentUserIdOrNull();
        UserAiConfigService.AiKey key = userAiConfigService.keyOf(userId);
        String answer = null;
        if (key != null) {
            StringBuilder kb = new StringBuilder();
            for (KnowledgeService.ScoredEntry se : top) {
                kb.append("问:").append(se.entry().getQuestion())
                  .append("\n答:").append(se.entry().getAnswer()).append("\n\n");
            }
            String sys = "你是乡村文旅智能助手「小乡」,语气亲切自然。请优先依据以下知识库回答游客问题,"
                    + "知识库没有时基于常识简要回答,并提醒可咨询景区工作人员。回答控制在150字内。";
            if (kb.length() > 0) sys += "\n\n知识库:\n" + kb;
            answer = aiClient.chatWith(key.apiKey(), key.baseUrl(), key.model(), sys, message);
        }
        // 本地轻量引擎兜底
        if (answer == null) {
            if (!top.isEmpty() && top.get(0).score() >= 3) {
                answer = top.get(0).entry().getAnswer();
            } else {
                answer = "这个问题我暂时还没有准备好答案。你可以问我门票、交通、住宿、美食、行程、乡村文化类的问题,"
                        + "也可以到「景区」页逛逛,说不定就有灵感了。";
            }
        }

        // 识别问题中提到的景区,便于前端一键跳转
        Long spotId = null;
        String spotName = null;
        for (ScenicSpot s : scenicSpotService.allActive()) {
            if (message.contains(s.getName()) || s.getName().contains(message)) {
                spotId = s.getId();
                spotName = s.getName();
                break;
            }
        }

        // 快捷追问建议
        List<String> suggestions = new ArrayList<>();
        for (KnowledgeService.ScoredEntry se : top) {
            if (suggestions.size() >= 3) break;
            if (!se.entry().getQuestion().equals(matchedQuestion)) {
                suggestions.add(se.entry().getQuestion());
            }
        }
        if (suggestions.isEmpty()) {
            suggestions.add("景区门票怎么收费?");
            suggestions.add("最佳旅游季节是什么时候?");
            suggestions.add("当地有哪些特色美食?");
        }
        return new ChatVO(answer, matchedQuestion, suggestions, spotId, spotName);
    }
}
