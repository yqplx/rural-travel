package com.rural.travel.controller;

import com.rural.travel.ai.AnalyticsService;
import com.rural.travel.ai.ContentGenerateService;
import com.rural.travel.ai.ImageRecognitionService;
import com.rural.travel.common.PageResult;
import com.rural.travel.common.Result;
import com.rural.travel.dto.ContentGenerateDTO;
import com.rural.travel.dto.KnowledgeDTO;
import com.rural.travel.entity.KnowledgeEntry;
import com.rural.travel.security.SecurityUtils;
import com.rural.travel.service.KnowledgeService;
import com.rural.travel.service.SysLogService;
import com.rural.travel.vo.AnalyticsVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 管理端-AI 知识库 / 数据分析 / 内容工作台 */
@RestController
@RequestMapping("/api/admin")
public class AdminAiController {

    private final KnowledgeService knowledgeService;
    private final AnalyticsService analyticsService;
    private final ContentGenerateService contentGenerateService;
    private final ImageRecognitionService imageRecognitionService;
    private final SysLogService sysLogService;

    public AdminAiController(KnowledgeService knowledgeService, AnalyticsService analyticsService,
                             ContentGenerateService contentGenerateService,
                             ImageRecognitionService imageRecognitionService,
                             SysLogService sysLogService) {
        this.knowledgeService = knowledgeService;
        this.analyticsService = analyticsService;
        this.contentGenerateService = contentGenerateService;
        this.imageRecognitionService = imageRecognitionService;
        this.sysLogService = sysLogService;
    }

    @GetMapping("/knowledge")
    public Result<PageResult<KnowledgeEntry>> knowledge(@RequestParam(defaultValue = "1") long current,
                                                        @RequestParam(defaultValue = "10") long size,
                                                        @RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) String category) {
        return Result.ok(knowledgeService.page(current, size, keyword, category));
    }

    @PostMapping("/knowledge")
    public Result<KnowledgeEntry> createKnowledge(@Valid @RequestBody KnowledgeDTO dto) {
        KnowledgeEntry e = knowledgeService.save(dto);
        sysLogService.save(SecurityUtils.currentUserId(), SecurityUtils.current().getUsername(),
                "新增知识", dto.getQuestion());
        return Result.ok(e);
    }

    @PutMapping("/knowledge/{id}")
    public Result<KnowledgeEntry> updateKnowledge(@PathVariable Long id,
                                                  @Valid @RequestBody KnowledgeDTO dto) {
        dto.setId(id);
        KnowledgeEntry e = knowledgeService.save(dto);
        sysLogService.save(SecurityUtils.currentUserId(), SecurityUtils.current().getUsername(),
                "编辑知识", dto.getQuestion());
        return Result.ok(e);
    }

    @DeleteMapping("/knowledge/{id}")
    public Result<Void> deleteKnowledge(@PathVariable Long id) {
        knowledgeService.delete(id);
        sysLogService.save(SecurityUtils.currentUserId(), SecurityUtils.current().getUsername(),
                "删除知识", "id=" + id);
        return Result.ok();
    }

    @GetMapping("/analytics")
    public Result<AnalyticsVO> analytics() {
        return Result.ok(analyticsService.analyze());
    }

    @PostMapping("/content/generate")
    public Result<Map<String, String>> generate(@Valid @RequestBody ContentGenerateDTO dto) {
        return Result.ok(contentGenerateService.generate(dto));
    }

    /** 手动触发识景哈希重建(抓取封面图计算真实pHash) */
    @PostMapping("/ai/rebuild-hashes")
    public Result<Void> rebuildHashes() {
        new Thread(imageRecognitionService::warmup).start();
        return Result.ok();
    }
}
