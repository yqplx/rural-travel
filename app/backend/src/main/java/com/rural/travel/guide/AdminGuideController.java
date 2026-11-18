package com.rural.travel.guide;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rural.travel.common.PageResult;
import com.rural.travel.common.Result;
import com.rural.travel.guide.dto.ContentBatchDTO;
import com.rural.travel.guide.dto.KgEntitySaveDTO;
import com.rural.travel.guide.dto.KgRelationSaveDTO;
import com.rural.travel.guide.entity.KgEntity;
import com.rural.travel.guide.entity.KgRelation;
import com.rural.travel.guide.entity.MmContent;
import com.rural.travel.guide.mapper.MmContentMapper;
import com.rural.travel.guide.vo.ContentProduceVO;
import com.rural.travel.guide.vo.PolicyMetricsVO;
import com.rural.travel.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 方案三 管理端：知识图谱维护、多模态内容量产、政策契合度看板 */
@RestController
@RequestMapping("/api/admin/guide")
@RequiredArgsConstructor
public class AdminGuideController {

    private final KgService kgService;
    private final MultimodalContentService contentService;
    private final PolicyMetricsService metricsService;
    private final MmContentMapper contentMapper;

    @GetMapping("/entities")
    public Result<PageResult<KgEntity>> entities(@RequestParam(defaultValue = "1") long current,
                                                 @RequestParam(defaultValue = "20") long size,
                                                 @RequestParam(required = false) String countyCode,
                                                 @RequestParam(required = false) String entityType,
                                                 @RequestParam(required = false) String keyword) {
        IPage<KgEntity> page = contentPage(current, size);
        LambdaQueryWrapper<KgEntity> w = new LambdaQueryWrapper<KgEntity>()
                .eq(StringUtils.hasText(countyCode), KgEntity::getCountyCode, countyCode)
                .eq(StringUtils.hasText(entityType), KgEntity::getEntityType, entityType)
                .and(StringUtils.hasText(keyword), k -> k.like(KgEntity::getName, keyword)
                        .or().like(KgEntity::getSummary, keyword))
                .orderByDesc(KgEntity::getUpdatedAt);
        return Result.ok(PageResult.of(kgPage(page, w)));
    }

    @PostMapping("/entities")
    public Result<KgEntity> saveEntity(@RequestBody KgEntitySaveDTO dto) {
        return Result.ok(kgService.save(dto));
    }

    @DeleteMapping("/entities/{id}")
    public Result<Boolean> deleteEntity(@PathVariable Long id) {
        kgService.remove(id);
        return Result.ok(true);
    }

    @GetMapping("/relations")
    public Result<List<KgRelation>> relations(@RequestParam(required = false) String countyCode) {
        return Result.ok(kgService.relations(countyCode));
    }

    @PostMapping("/relations")
    public Result<KgRelation> saveRelation(@RequestBody KgRelationSaveDTO dto) {
        return Result.ok(kgService.saveRelation(dto));
    }

    @DeleteMapping("/relations/{id}")
    public Result<Boolean> deleteRelation(@PathVariable Long id) {
        kgService.removeRelation(id);
        return Result.ok(true);
    }

    /** S2 一键批量生产多模态内容 */
    @PostMapping("/content/produce")
    public Result<ContentProduceVO> produce(@RequestBody ContentBatchDTO dto) {
        return Result.ok(contentService.produce(dto.getEntityId(), dto, SecurityUtils.currentUserId()));
    }

    /** 全县域批量生产：遍历所有实体，支撑"低成本内容量产" */
    @PostMapping("/content/produce-all")
    public Result<List<ContentProduceVO>> produceAll(@RequestParam(required = false) String countyCode,
                                                     @RequestBody(required = false) ContentBatchDTO dto) {
        ContentBatchDTO d = dto == null ? new ContentBatchDTO() : dto;
        String code = StringUtils.hasText(countyCode) ? countyCode : kgService.defaultCounty();
        List<ContentProduceVO> list = new java.util.ArrayList<>();
        for (KgEntity e : kgService.list(code, null)) {
            d.setEntityId(e.getId());
            list.add(contentService.produce(e.getId(), d, SecurityUtils.currentUserId()));
        }
        return Result.ok(list);
    }

    @GetMapping("/contents")
    public Result<PageResult<MmContent>> contents(@RequestParam(defaultValue = "1") long current,
                                                  @RequestParam(defaultValue = "20") long size,
                                                  @RequestParam(required = false) Long entityId,
                                                  @RequestParam(required = false) String contentType) {
        IPage<MmContent> p = contentMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<MmContent>()
                        .eq(entityId != null, MmContent::getEntityId, entityId)
                        .eq(StringUtils.hasText(contentType), MmContent::getContentType, contentType)
                        .orderByDesc(MmContent::getCreatedAt));
        return Result.ok(PageResult.of(p));
    }

    @DeleteMapping("/contents/{id}")
    public Result<Boolean> deleteContent(@PathVariable Long id) {
        MmContent c = contentMapper.selectById(id);
        contentMapper.deleteById(id);
        if (c != null) kgService.recalcContentScore(c.getEntityId());
        return Result.ok(true);
    }

    /** 政策契合度指标 */
    @GetMapping("/metrics")
    public Result<PolicyMetricsVO> metrics() {
        return Result.ok(metricsService.metrics());
    }

    // --- 分页查询私有辅助：避免 Page 泛型误用 ---
    private IPage<KgEntity> contentPage(long current, long size) {
        return new Page<>(current, size);
    }

    private IPage<KgEntity> kgPage(IPage<KgEntity> page, LambdaQueryWrapper<KgEntity> w) {
        return kgService.page(page, w);
    }
}
