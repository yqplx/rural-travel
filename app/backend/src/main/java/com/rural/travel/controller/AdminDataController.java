package com.rural.travel.controller;

import com.rural.travel.common.PageResult;
import com.rural.travel.common.Result;
import com.rural.travel.dto.VirtualRatingSaveDTO;
import com.rural.travel.entity.Rating;
import com.rural.travel.entity.SysLog;
import com.rural.travel.entity.VirtualRating;
import com.rural.travel.entity.VirtualUser;
import com.rural.travel.service.RatingService;
import com.rural.travel.service.SysLogService;
import com.rural.travel.service.VirtualDataService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 管理端-评分数据 / 虚拟样本 / 系统日志 */
@RestController
@RequestMapping("/api/admin")
public class AdminDataController {

    private final RatingService ratingService;
    private final VirtualDataService virtualDataService;
    private final SysLogService sysLogService;

    public AdminDataController(RatingService ratingService, VirtualDataService virtualDataService,
                               SysLogService sysLogService) {
        this.ratingService = ratingService;
        this.virtualDataService = virtualDataService;
        this.sysLogService = sysLogService;
    }

    @GetMapping("/ratings")
    public Result<PageResult<Rating>> ratings(@RequestParam(defaultValue = "1") long current,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) Long scenicSpotId) {
        return Result.ok(ratingService.adminPage(current, size, scenicSpotId));
    }

    @DeleteMapping("/ratings/{id}")
    public Result<Void> deleteRating(@PathVariable Long id) {
        ratingService.adminDelete(id);
        return Result.ok();
    }

    @GetMapping("/virtual-users")
    public Result<PageResult<VirtualUser>> virtualUsers(@RequestParam(defaultValue = "1") long current,
                                                        @RequestParam(defaultValue = "10") long size,
                                                        @RequestParam(required = false) String keyword) {
        return Result.ok(virtualDataService.pageUsers(current, size, keyword));
    }

    @GetMapping("/virtual-users/all")
    public Result<List<VirtualUser>> allVirtualUsers() {
        return Result.ok(virtualDataService.allUsers());
    }

    @PostMapping("/virtual-users")
    public Result<VirtualUser> saveVirtualUser(@RequestBody VirtualUser u) {
        return Result.ok(virtualDataService.saveUser(u));
    }

    @PutMapping("/virtual-users/{id}")
    public Result<VirtualUser> updateVirtualUser(@PathVariable Long id, @RequestBody VirtualUser u) {
        u.setId(id);
        return Result.ok(virtualDataService.saveUser(u));
    }

    @DeleteMapping("/virtual-users/{id}")
    public Result<Void> deleteVirtualUser(@PathVariable Long id) {
        virtualDataService.deleteUser(id);
        return Result.ok();
    }

    @GetMapping("/virtual-ratings")
    public Result<PageResult<VirtualRating>> virtualRatings(@RequestParam(defaultValue = "1") long current,
                                                            @RequestParam(defaultValue = "10") long size,
                                                            @RequestParam(required = false) Long virtualUserId) {
        return Result.ok(virtualDataService.pageRatings(current, size, virtualUserId));
    }

    @PostMapping("/virtual-ratings")
    public Result<VirtualRating> saveVirtualRating(@Valid @RequestBody VirtualRatingSaveDTO dto) {
        return Result.ok(virtualDataService.saveRating(dto));
    }

    @PutMapping("/virtual-ratings/{id}")
    public Result<VirtualRating> updateVirtualRating(@PathVariable Long id,
                                                     @Valid @RequestBody VirtualRatingSaveDTO dto) {
        dto.setId(id);
        return Result.ok(virtualDataService.saveRating(dto));
    }

    @DeleteMapping("/virtual-ratings/{id}")
    public Result<Void> deleteVirtualRating(@PathVariable Long id) {
        virtualDataService.deleteRating(id);
        return Result.ok();
    }

    @GetMapping("/logs")
    public Result<PageResult<SysLog>> logs(@RequestParam(defaultValue = "1") long current,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) String keyword) {
        return Result.ok(sysLogService.page(current, size, keyword));
    }
}
