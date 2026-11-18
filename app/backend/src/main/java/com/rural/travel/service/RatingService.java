package com.rural.travel.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rural.travel.common.PageResult;
import com.rural.travel.dto.RatingDTO;
import com.rural.travel.entity.Rating;
import com.rural.travel.entity.ScenicSpot;
import com.rural.travel.entity.User;
import com.rural.travel.entity.VirtualRating;
import com.rural.travel.exception.BizException;
import com.rural.travel.mapper.RatingMapper;
import com.rural.travel.mapper.UserMapper;
import com.rural.travel.mapper.VirtualRatingMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RatingService {

    private final RatingMapper ratingMapper;
    private final VirtualRatingMapper virtualRatingMapper;
    private final ScenicSpotService scenicSpotService;
    private final InterestService interestService;
    private final UserMapper userMapper;

    public RatingService(RatingMapper ratingMapper, VirtualRatingMapper virtualRatingMapper,
                         ScenicSpotService scenicSpotService, InterestService interestService,
                         UserMapper userMapper) {
        this.ratingMapper = ratingMapper;
        this.virtualRatingMapper = virtualRatingMapper;
        this.scenicSpotService = scenicSpotService;
        this.interestService = interestService;
        this.userMapper = userMapper;
    }

    public Rating submit(Long userId, RatingDTO dto) {
        ScenicSpot spot = scenicSpotService.getById(dto.scenicSpotId());
        Rating rating = ratingMapper.selectOne(new QueryWrapper<Rating>()
                .eq("user_id", userId).eq("scenic_spot_id", dto.scenicSpotId()));
        if (rating == null) {
            rating = new Rating();
            rating.setUserId(userId);
            rating.setScenicSpotId(dto.scenicSpotId());
            rating.setScore(dto.score());
            rating.setComment(dto.comment());
            ratingMapper.insert(rating);
        } else {
            rating.setScore(dto.score());
            rating.setComment(dto.comment());
            ratingMapper.updateById(rating);
        }
        // 评分越高,兴趣权重累积越多
        interestService.record(userId, spot, dto.score() * 0.8);
        refreshSpotScore(dto.scenicSpotId());
        return rating;
    }

    /** 综合真实评分与虚拟样本重算景区均分 */
    public void refreshSpotScore(Long spotId) {
        List<Rating> real = ratingMapper.selectList(new QueryWrapper<Rating>().eq("scenic_spot_id", spotId));
        List<VirtualRating> virtual = virtualRatingMapper.selectList(
                new QueryWrapper<VirtualRating>().eq("scenic_spot_id", spotId));
        int count = real.size() + virtual.size();
        if (count == 0) {
            scenicSpotService.refreshAvgScore(spotId, 0.0, 0);
            return;
        }
        double sum = real.stream().mapToInt(Rating::getScore).sum()
                + virtual.stream().mapToInt(VirtualRating::getScore).sum();
        scenicSpotService.refreshAvgScore(spotId, sum / count, count);
    }

    public Integer myScore(Long userId, Long scenicSpotId) {
        if (userId == null) return null;
        Rating r = ratingMapper.selectOne(new QueryWrapper<Rating>()
                .eq("user_id", userId).eq("scenic_spot_id", scenicSpotId));
        return r == null ? null : r.getScore();
    }

    public List<Rating> ofSpot(Long scenicSpotId) {
        List<Rating> list = ratingMapper.selectList(new QueryWrapper<Rating>()
                .eq("scenic_spot_id", scenicSpotId).orderByDesc("id").last("LIMIT 20"));
        fillUser(list);
        return list;
    }

    public PageResult<Rating> myPage(Long userId, long current, long size) {
        Page<Rating> page = ratingMapper.selectPage(new Page<>(current, size),
                new QueryWrapper<Rating>().eq("user_id", userId).orderByDesc("id"));
        page.getRecords().forEach(r -> r.setScenicName(
                scenicSpotService.getById(r.getScenicSpotId()).getName()));
        return PageResult.of(page);
    }

    // ==================== 管理端 ====================

    public PageResult<Rating> adminPage(long current, long size, Long scenicSpotId) {
        Page<Rating> page = ratingMapper.selectPage(new Page<>(current, size),
                new QueryWrapper<Rating>()
                        .eq(scenicSpotId != null, "scenic_spot_id", scenicSpotId)
                        .orderByDesc("id"));
        fillUser(page.getRecords());
        page.getRecords().forEach(r -> r.setScenicName(
                scenicSpotService.getById(r.getScenicSpotId()).getName()));
        return PageResult.of(page);
    }

    public void adminDelete(Long id) {
        Rating r = ratingMapper.selectById(id);
        if (r == null) throw new BizException(404, "评分不存在");
        ratingMapper.deleteById(id);
        refreshSpotScore(r.getScenicSpotId());
    }

    private void fillUser(List<Rating> list) {
        if (list.isEmpty()) return;
        Map<Long, User> users = userMapper.selectBatchIds(
                        list.stream().map(Rating::getUserId).distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        for (Rating r : list) {
            User u = users.get(r.getUserId());
            if (u != null) {
                r.setUsername(u.getUsername());
                r.setNickname(u.getNickname());
            }
        }
    }
}
