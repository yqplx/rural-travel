package com.rural.travel.controller;

import com.rural.travel.common.PageResult;
import com.rural.travel.common.Result;
import com.rural.travel.entity.Favorite;
import com.rural.travel.security.SecurityUtils;
import com.rural.travel.service.FavoriteService;
import org.springframework.web.bind.annotation.*;

/** 收藏 */
@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping("/{scenicSpotId}")
    public Result<Void> add(@PathVariable Long scenicSpotId) {
        favoriteService.add(SecurityUtils.currentUserId(), scenicSpotId);
        return Result.ok();
    }

    @DeleteMapping("/{scenicSpotId}")
    public Result<Void> remove(@PathVariable Long scenicSpotId) {
        favoriteService.remove(SecurityUtils.currentUserId(), scenicSpotId);
        return Result.ok();
    }

    @GetMapping
    public Result<PageResult<Favorite>> mine(@RequestParam(defaultValue = "1") long current,
                                             @RequestParam(defaultValue = "10") long size) {
        return Result.ok(favoriteService.pageMine(SecurityUtils.currentUserId(), current, size));
    }
}
