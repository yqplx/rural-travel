package com.rural.travel.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rural.travel.common.PageResult;
import com.rural.travel.entity.Favorite;
import com.rural.travel.entity.ScenicSpot;
import com.rural.travel.exception.BizException;
import com.rural.travel.mapper.FavoriteMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final ScenicSpotService scenicSpotService;
    private final InterestService interestService;

    public FavoriteService(FavoriteMapper favoriteMapper, ScenicSpotService scenicSpotService,
                           InterestService interestService) {
        this.favoriteMapper = favoriteMapper;
        this.scenicSpotService = scenicSpotService;
        this.interestService = interestService;
    }

    public void add(Long userId, Long scenicSpotId) {
        ScenicSpot spot = scenicSpotService.getById(scenicSpotId);
        Long exists = favoriteMapper.selectCount(new QueryWrapper<Favorite>()
                .eq("user_id", userId).eq("scenic_spot_id", scenicSpotId));
        if (exists > 0) throw new BizException("已在收藏列表中");
        Favorite f = new Favorite();
        f.setUserId(userId);
        f.setScenicSpotId(scenicSpotId);
        favoriteMapper.insert(f);
        interestService.record(userId, spot, 2.0);
    }

    public void remove(Long userId, Long scenicSpotId) {
        favoriteMapper.delete(new QueryWrapper<Favorite>()
                .eq("user_id", userId).eq("scenic_spot_id", scenicSpotId));
    }

    public boolean isFavorited(Long userId, Long scenicSpotId) {
        if (userId == null) return false;
        return favoriteMapper.selectCount(new QueryWrapper<Favorite>()
                .eq("user_id", userId).eq("scenic_spot_id", scenicSpotId)) > 0;
    }

    public PageResult<Favorite> pageMine(Long userId, long current, long size) {
        Page<Favorite> page = favoriteMapper.selectPage(new Page<>(current, size),
                new QueryWrapper<Favorite>().eq("user_id", userId).orderByDesc("id"));
        List<Favorite> records = page.getRecords();
        if (!records.isEmpty()) {
            Map<Long, ScenicSpot> spotMap = records.stream()
                    .map(f -> scenicSpotService.getById(f.getScenicSpotId()))
                    .collect(Collectors.toMap(ScenicSpot::getId, Function.identity()));
            records.forEach(f -> f.setScenicSpot(spotMap.get(f.getScenicSpotId())));
            scenicSpotService.enrich(records.stream().map(Favorite::getScenicSpot).collect(Collectors.toList()));
        }
        return PageResult.of(page);
    }
}
