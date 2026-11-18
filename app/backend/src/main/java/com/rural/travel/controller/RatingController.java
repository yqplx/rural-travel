package com.rural.travel.controller;

import com.rural.travel.common.PageResult;
import com.rural.travel.common.Result;
import com.rural.travel.dto.RatingDTO;
import com.rural.travel.entity.Rating;
import com.rural.travel.security.SecurityUtils;
import com.rural.travel.service.RatingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** 评分 */
@RestController
@RequestMapping("/api/ratings")
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PostMapping
    public Result<Rating> submit(@Valid @RequestBody RatingDTO dto) {
        return Result.ok(ratingService.submit(SecurityUtils.currentUserId(), dto));
    }

    @GetMapping("/my")
    public Result<PageResult<Rating>> my(@RequestParam(defaultValue = "1") long current,
                                         @RequestParam(defaultValue = "10") long size) {
        return Result.ok(ratingService.myPage(SecurityUtils.currentUserId(), current, size));
    }
}
