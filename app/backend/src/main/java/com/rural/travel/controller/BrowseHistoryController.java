package com.rural.travel.controller;

import com.rural.travel.common.PageResult;
import com.rural.travel.common.Result;
import com.rural.travel.entity.BrowseHistory;
import com.rural.travel.security.SecurityUtils;
import com.rural.travel.service.BrowseHistoryService;
import org.springframework.web.bind.annotation.*;

/** 浏览历史 */
@RestController
@RequestMapping("/api/browse-history")
public class BrowseHistoryController {

    private final BrowseHistoryService browseHistoryService;

    public BrowseHistoryController(BrowseHistoryService browseHistoryService) {
        this.browseHistoryService = browseHistoryService;
    }

    public record BrowseDTO(Long scenicSpotId, Integer duration) {}

    @PostMapping
    public Result<Void> record(@RequestBody BrowseDTO dto) {
        browseHistoryService.record(SecurityUtils.currentUserId(), dto.scenicSpotId(), dto.duration());
        return Result.ok();
    }

    @GetMapping
    public Result<PageResult<BrowseHistory>> mine(@RequestParam(defaultValue = "1") long current,
                                                  @RequestParam(defaultValue = "10") long size) {
        return Result.ok(browseHistoryService.pageMine(SecurityUtils.currentUserId(), current, size));
    }

    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        browseHistoryService.remove(SecurityUtils.currentUserId(), id);
        return Result.ok();
    }

    @DeleteMapping
    public Result<Void> clear() {
        browseHistoryService.clear(SecurityUtils.currentUserId());
        return Result.ok();
    }
}
