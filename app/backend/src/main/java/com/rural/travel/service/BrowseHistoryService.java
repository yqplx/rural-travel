package com.rural.travel.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rural.travel.common.PageResult;
import com.rural.travel.entity.BrowseHistory;
import com.rural.travel.entity.ScenicSpot;
import com.rural.travel.mapper.BrowseHistoryMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BrowseHistoryService {

    private final BrowseHistoryMapper browseHistoryMapper;
    private final ScenicSpotService scenicSpotService;
    private final InterestService interestService;

    public BrowseHistoryService(BrowseHistoryMapper browseHistoryMapper,
                                ScenicSpotService scenicSpotService,
                                InterestService interestService) {
        this.browseHistoryMapper = browseHistoryMapper;
        this.scenicSpotService = scenicSpotService;
        this.interestService = interestService;
    }

    /** 记录浏览(5分钟内同景区去重) */
    public void record(Long userId, Long scenicSpotId, Integer duration) {
        if (userId == null) return;
        ScenicSpot spot = scenicSpotService.getById(scenicSpotId);
        BrowseHistory recent = browseHistoryMapper.selectOne(new QueryWrapper<BrowseHistory>()
                .eq("user_id", userId).eq("scenic_spot_id", scenicSpotId)
                .ge("created_at", LocalDateTime.now().minusMinutes(5))
                .orderByDesc("id").last("LIMIT 1"));
        if (recent != null) {
            recent.setDuration((recent.getDuration() == null ? 0 : recent.getDuration())
                    + (duration == null ? 0 : duration));
            browseHistoryMapper.updateById(recent);
            return;
        }
        BrowseHistory h = new BrowseHistory();
        h.setUserId(userId);
        h.setScenicSpotId(scenicSpotId);
        h.setDuration(duration == null ? 0 : duration);
        browseHistoryMapper.insert(h);
        interestService.record(userId, spot, 0.5);
    }

    public PageResult<BrowseHistory> pageMine(Long userId, long current, long size) {
        Page<BrowseHistory> page = browseHistoryMapper.selectPage(new Page<>(current, size),
                new QueryWrapper<BrowseHistory>().eq("user_id", userId).orderByDesc("id"));
        List<BrowseHistory> records = page.getRecords();
        if (!records.isEmpty()) {
            Map<Long, ScenicSpot> spotMap = records.stream()
                    .map(h -> scenicSpotService.getById(h.getScenicSpotId()))
                    .collect(Collectors.toMap(ScenicSpot::getId, Function.identity(), (a, b) -> a));
            records.forEach(h -> h.setScenicSpot(spotMap.get(h.getScenicSpotId())));
            scenicSpotService.enrich(records.stream().map(BrowseHistory::getScenicSpot)
                    .filter(java.util.Objects::nonNull).collect(Collectors.toList()));
        }
        return PageResult.of(page);
    }

    public void remove(Long userId, Long id) {
        browseHistoryMapper.delete(new QueryWrapper<BrowseHistory>()
                .eq("user_id", userId).eq("id", id));
    }

    public void clear(Long userId) {
        browseHistoryMapper.delete(new QueryWrapper<BrowseHistory>().eq("user_id", userId));
    }
}
