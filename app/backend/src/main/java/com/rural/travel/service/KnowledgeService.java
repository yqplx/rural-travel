package com.rural.travel.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rural.travel.common.PageResult;
import com.rural.travel.dto.KnowledgeDTO;
import com.rural.travel.entity.KnowledgeEntry;
import com.rural.travel.exception.BizException;
import com.rural.travel.mapper.KnowledgeEntryMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/** AI 问答知识库 */
@Service
public class KnowledgeService {

    private final KnowledgeEntryMapper knowledgeEntryMapper;

    public KnowledgeService(KnowledgeEntryMapper knowledgeEntryMapper) {
        this.knowledgeEntryMapper = knowledgeEntryMapper;
    }

    public record ScoredEntry(KnowledgeEntry entry, double score) {}

    /** 关键词命中(≥2字词3分) + 二元组重合(0.5分/个) 双重打分 */
    public List<ScoredEntry> search(String message, int topN) {
        List<KnowledgeEntry> all = knowledgeEntryMapper.selectList(
                new QueryWrapper<KnowledgeEntry>().eq("status", 1).orderByAsc("sort_order"));
        List<ScoredEntry> scored = new ArrayList<>();
        for (KnowledgeEntry e : all) {
            double score = 0;
            if (StringUtils.hasText(e.getKeywords())) {
                for (String kw : e.getKeywords().split(",")) {
                    kw = kw.trim();
                    if (kw.length() >= 2 && message.contains(kw)) score += 3;
                }
            }
            String q = e.getQuestion() == null ? "" : e.getQuestion();
            for (int i = 0; i + 2 <= q.length(); i++) {
                String bg = q.substring(i, i + 2);
                if (message.contains(bg)) score += 0.5;
            }
            if (score > 0) scored.add(new ScoredEntry(e, score));
        }
        scored.sort((a, b) -> Double.compare(b.score(), a.score()));
        return scored.stream().limit(topN).toList();
    }

    // ==================== 管理端 ====================

    public PageResult<KnowledgeEntry> page(long current, long size, String keyword, String category) {
        Page<KnowledgeEntry> page = knowledgeEntryMapper.selectPage(new Page<>(current, size),
                new QueryWrapper<KnowledgeEntry>()
                        .like(StringUtils.hasText(keyword), "question", keyword)
                        .eq(StringUtils.hasText(category), "category", category)
                        .orderByAsc("sort_order").orderByDesc("id"));
        return PageResult.of(page);
    }

    public KnowledgeEntry save(KnowledgeDTO dto) {
        KnowledgeEntry e = dto.getId() == null ? new KnowledgeEntry()
                : knowledgeEntryMapper.selectById(dto.getId());
        if (e == null) throw new BizException(404, "知识条目不存在");
        e.setQuestion(dto.getQuestion());
        e.setAnswer(dto.getAnswer());
        e.setKeywords(dto.getKeywords());
        e.setCategory(StringUtils.hasText(dto.getCategory()) ? dto.getCategory() : "通用");
        e.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        e.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        if (dto.getId() == null) knowledgeEntryMapper.insert(e); else knowledgeEntryMapper.updateById(e);
        return e;
    }

    public void delete(Long id) {
        knowledgeEntryMapper.deleteById(id);
    }
}
