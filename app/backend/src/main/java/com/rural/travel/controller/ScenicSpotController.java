package com.rural.travel.controller;

import com.rural.travel.common.PageResult;
import com.rural.travel.common.Result;
import com.rural.travel.dto.ScenicQueryDTO;
import com.rural.travel.entity.Category;
import com.rural.travel.entity.Rating;
import com.rural.travel.entity.ScenicSpot;
import com.rural.travel.entity.Tag;
import com.rural.travel.recommend.RecommendService;
import com.rural.travel.ai.VoiceGuideService;
import com.rural.travel.security.SecurityUtils;
import com.rural.travel.service.BrowseHistoryService;
import com.rural.travel.service.DictService;
import com.rural.travel.service.FavoriteService;
import com.rural.travel.service.RatingService;
import com.rural.travel.service.ScenicSpotService;
import com.rural.travel.vo.VoiceGuideVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 景区公开查询(游客可访问) */
@RestController
@RequestMapping("/api")
public class ScenicSpotController {

    private final ScenicSpotService scenicSpotService;
    private final DictService dictService;
    private final FavoriteService favoriteService;
    private final RatingService ratingService;
    private final BrowseHistoryService browseHistoryService;
    private final RecommendService recommendService;
    private final VoiceGuideService voiceGuideService;

    public ScenicSpotController(ScenicSpotService scenicSpotService, DictService dictService,
                                FavoriteService favoriteService, RatingService ratingService,
                                BrowseHistoryService browseHistoryService,
                                RecommendService recommendService, VoiceGuideService voiceGuideService) {
        this.scenicSpotService = scenicSpotService;
        this.dictService = dictService;
        this.favoriteService = favoriteService;
        this.ratingService = ratingService;
        this.browseHistoryService = browseHistoryService;
        this.recommendService = recommendService;
        this.voiceGuideService = voiceGuideService;
    }

    @GetMapping("/scenic-spots")
    public Result<PageResult<ScenicSpot>> page(ScenicQueryDTO query) {
        return Result.ok(scenicSpotService.page(query));
    }

    @GetMapping("/scenic-spots/{id}")
    public Result<ScenicSpot> detail(@PathVariable Long id,
                                     @RequestParam(required = false) Integer duration) {
        Long userId = SecurityUtils.currentUserIdOrNull();
        ScenicSpot spot = scenicSpotService.detail(id);
        if (userId != null) {
            browseHistoryService.record(userId, id, duration == null ? 30 : duration);
            spot.setFavorited(favoriteService.isFavorited(userId, id));
            spot.setMyScore(ratingService.myScore(userId, id));
        }
        return Result.ok(spot);
    }

    @GetMapping("/scenic-spots/{id}/ratings")
    public Result<List<Rating>> ratings(@PathVariable Long id) {
        return Result.ok(ratingService.ofSpot(id));
    }

    /** 详情页「猜你喜欢」 */
    @GetMapping("/scenic-spots/{id}/similar")
    public Result<List<ScenicSpot>> similar(@PathVariable Long id) {
        return Result.ok(recommendService.similarSpots(id, 6));
    }

    /** 语音导游讲解稿(游客可用) */
    @GetMapping("/scenic-spots/{id}/voice-guide")
    public Result<VoiceGuideVO> voiceGuide(@PathVariable Long id) {
        return Result.ok(voiceGuideService.guide(id));
    }

    @GetMapping("/categories")
    public Result<List<Category>> categories() {
        return Result.ok(dictService.listCategories());
    }

    @GetMapping("/tags")
    public Result<List<Tag>> tags() {
        return Result.ok(dictService.listTags());
    }
}
