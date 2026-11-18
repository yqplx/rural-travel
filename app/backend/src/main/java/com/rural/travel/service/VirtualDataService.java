package com.rural.travel.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rural.travel.common.PageResult;
import com.rural.travel.dto.VirtualRatingSaveDTO;
import com.rural.travel.entity.VirtualRating;
import com.rural.travel.entity.VirtualUser;
import com.rural.travel.exception.BizException;
import com.rural.travel.mapper.VirtualRatingMapper;
import com.rural.travel.mapper.VirtualUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 虚拟用户与虚拟评分样本管理(供推荐引擎冷启动) */
@Service
public class VirtualDataService {

    private final VirtualUserMapper virtualUserMapper;
    private final VirtualRatingMapper virtualRatingMapper;
    private final RatingService ratingService;

    public VirtualDataService(VirtualUserMapper virtualUserMapper, VirtualRatingMapper virtualRatingMapper,
                              RatingService ratingService) {
        this.virtualUserMapper = virtualUserMapper;
        this.virtualRatingMapper = virtualRatingMapper;
        this.ratingService = ratingService;
    }

    public PageResult<VirtualUser> pageUsers(long current, long size, String keyword) {
        Page<VirtualUser> page = virtualUserMapper.selectPage(new Page<>(current, size),
                new QueryWrapper<VirtualUser>()
                        .like(StringUtils.hasText(keyword), "name", keyword)
                        .orderByDesc("id"));
        return PageResult.of(page);
    }

    public List<VirtualUser> allUsers() {
        return virtualUserMapper.selectList(new QueryWrapper<VirtualUser>().orderByAsc("id"));
    }

    public VirtualUser saveUser(VirtualUser u) {
        if (!StringUtils.hasText(u.getName())) throw new BizException("请输入虚拟用户名称");
        if (u.getId() == null) virtualUserMapper.insert(u); else virtualUserMapper.updateById(u);
        return u;
    }

    @Transactional
    public void deleteUser(Long id) {
        virtualUserMapper.deleteById(id);
        List<VirtualRating> ratings = virtualRatingMapper.selectList(
                new QueryWrapper<VirtualRating>().eq("virtual_user_id", id));
        virtualRatingMapper.delete(new QueryWrapper<VirtualRating>().eq("virtual_user_id", id));
        ratings.forEach(r -> ratingService.refreshSpotScore(r.getScenicSpotId()));
    }

    public PageResult<VirtualRating> pageRatings(long current, long size, Long virtualUserId) {
        Page<VirtualRating> page = virtualRatingMapper.selectPage(new Page<>(current, size),
                new QueryWrapper<VirtualRating>()
                        .eq(virtualUserId != null, "virtual_user_id", virtualUserId)
                        .orderByDesc("id"));
        Map<Long, VirtualUser> users = allUsers().stream()
                .collect(Collectors.toMap(VirtualUser::getId, Function.identity()));
        page.getRecords().forEach(r -> {
            VirtualUser u = users.get(r.getVirtualUserId());
            if (u != null) r.setVirtualUserName(u.getName());
        });
        return PageResult.of(page);
    }

    public VirtualRating saveRating(VirtualRatingSaveDTO dto) {
        VirtualRating r = dto.getId() == null ? new VirtualRating()
                : virtualRatingMapper.selectById(dto.getId());
        if (r == null) throw new BizException(404, "样本不存在");
        r.setVirtualUserId(dto.getVirtualUserId());
        r.setScenicSpotId(dto.getScenicSpotId());
        r.setScore(dto.getScore());
        if (dto.getId() == null) virtualRatingMapper.insert(r); else virtualRatingMapper.updateById(r);
        ratingService.refreshSpotScore(dto.getScenicSpotId());
        return r;
    }

    public void deleteRating(Long id) {
        VirtualRating r = virtualRatingMapper.selectById(id);
        if (r == null) throw new BizException(404, "样本不存在");
        virtualRatingMapper.deleteById(id);
        ratingService.refreshSpotScore(r.getScenicSpotId());
    }
}
