package com.rural.travel.controller;

import com.rural.travel.common.Result;
import com.rural.travel.entity.ScenicSpot;
import com.rural.travel.recommend.RecommendService;
import com.rural.travel.security.SecurityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 推荐(前端仅展示自然化文案) */
@RestController
@RequestMapping("/api/recommendations")
public class RecommendController {

    private final RecommendService recommendService;

    public RecommendController(RecommendService recommendService) {
        this.recommendService = recommendService;
    }

    @GetMapping
    public Result<List<ScenicSpot>> recommend(@RequestParam(defaultValue = "10") int limit) {
        // 游客走热门榜单,登录用户走个性化推荐;游客也放行,体验更顺滑
        return Result.ok(recommendService.recommendFor(SecurityUtils.currentUserIdOrNull(), limit));
    }
}
