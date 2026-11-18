package com.rural.travel.controller;

import com.rural.travel.common.PageResult;
import com.rural.travel.common.Result;
import com.rural.travel.dto.ScenicSaveDTO;
import com.rural.travel.entity.ScenicSpot;
import com.rural.travel.security.SecurityUtils;
import com.rural.travel.service.ScenicSpotService;
import com.rural.travel.service.SysLogService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** 管理端-景区管理 */
@RestController
@RequestMapping("/api/admin/scenic-spots")
public class AdminScenicController {

    private final ScenicSpotService scenicSpotService;
    private final SysLogService sysLogService;

    public AdminScenicController(ScenicSpotService scenicSpotService, SysLogService sysLogService) {
        this.scenicSpotService = scenicSpotService;
        this.sysLogService = sysLogService;
    }

    @GetMapping
    public Result<PageResult<ScenicSpot>> page(@RequestParam(defaultValue = "1") long current,
                                               @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) Long categoryId,
                                               @RequestParam(required = false) Integer status) {
        return Result.ok(scenicSpotService.adminPage(current, size, keyword, categoryId, status));
    }

    @GetMapping("/{id}")
    public Result<ScenicSpot> detail(@PathVariable Long id) {
        return Result.ok(scenicSpotService.detail(id));
    }

    @PostMapping
    public Result<ScenicSpot> create(@Valid @RequestBody ScenicSaveDTO dto) {
        ScenicSpot s = scenicSpotService.save(dto);
        sysLogService.save(SecurityUtils.currentUserId(), SecurityUtils.current().getUsername(),
                "新增景区", s.getName());
        return Result.ok(s);
    }

    @PutMapping("/{id}")
    public Result<ScenicSpot> update(@PathVariable Long id, @Valid @RequestBody ScenicSaveDTO dto) {
        dto.setId(id);
        ScenicSpot s = scenicSpotService.save(dto);
        sysLogService.save(SecurityUtils.currentUserId(), SecurityUtils.current().getUsername(),
                "编辑景区", s.getName());
        return Result.ok(s);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        String name = scenicSpotService.getById(id).getName();
        scenicSpotService.delete(id);
        sysLogService.save(SecurityUtils.currentUserId(), SecurityUtils.current().getUsername(),
                "删除景区", name);
        return Result.ok();
    }
}
