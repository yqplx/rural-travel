package com.rural.travel.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rural.travel.entity.ScenicSpot;
import com.rural.travel.entity.UserInterest;
import com.rural.travel.mapper.UserInterestMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 用户兴趣画像:随浏览/收藏/评分行为累积权重,支持用户主动勾选偏好 */
@Service
public class InterestService {

    private final UserInterestMapper userInterestMapper;

    public InterestService(UserInterestMapper userInterestMapper) {
        this.userInterestMapper = userInterestMapper;
    }

    public void record(Long userId, ScenicSpot spot, double weight) {
        if (userId == null || spot == null) return;
        bump(userId, spot.getCategoryId(), null, weight);
        for (Long tagId : ScenicSpotService.parseTagIds(spot.getTagIds())) {
            bump(userId, null, tagId, weight);
        }
    }

    private void bump(Long userId, Long categoryId, Long tagId, double delta) {
        UserInterest ui = userInterestMapper.selectOne(new QueryWrapper<UserInterest>()
                .eq("user_id", userId)
                .eq(categoryId != null, "category_id", categoryId)
                .isNull(categoryId == null, "category_id")
                .eq(tagId != null, "tag_id", tagId)
                .isNull(tagId == null, "tag_id"));
        if (ui == null) {
            ui = new UserInterest();
            ui.setUserId(userId);
            ui.setCategoryId(categoryId);
            ui.setTagId(tagId);
            ui.setWeight(delta);
            userInterestMapper.insert(ui);
        } else {
            ui.setWeight(Math.min(ui.getWeight() + delta, 50.0));
            userInterestMapper.updateById(ui);
        }
    }

    public Map<Long, Double> tagWeights(Long userId) {
        Map<Long, Double> map = new HashMap<>();
        for (UserInterest ui : userInterestMapper.selectList(new QueryWrapper<UserInterest>()
                .eq("user_id", userId).isNotNull("tag_id"))) {
            map.put(ui.getTagId(), ui.getWeight());
        }
        return map;
    }

    public Map<Long, Double> categoryWeights(Long userId) {
        Map<Long, Double> map = new HashMap<>();
        for (UserInterest ui : userInterestMapper.selectList(new QueryWrapper<UserInterest>()
                .eq("user_id", userId).isNotNull("category_id"))) {
            map.put(ui.getCategoryId(), ui.getWeight());
        }
        return map;
    }

    public List<UserInterest> list(Long userId) {
        return userInterestMapper.selectList(new QueryWrapper<UserInterest>()
                .eq("user_id", userId).orderByDesc("weight"));
    }

    /** 当前用户勾选的偏好标签id */
    public List<Long> tagIdsOf(Long userId) {
        return userInterestMapper.selectList(new QueryWrapper<UserInterest>()
                        .eq("user_id", userId).isNotNull("tag_id"))
                .stream().map(UserInterest::getTagId).distinct().collect(Collectors.toList());
    }

    /** 用户主动勾选偏好:整体替换并赋予较高初始权重 */
    @Transactional
    public void setInterests(Long userId, List<Long> tagIds) {
        userInterestMapper.delete(new QueryWrapper<UserInterest>().eq("user_id", userId));
        if (tagIds == null) return;
        for (Long tagId : tagIds.stream().distinct().collect(Collectors.toList())) {
            UserInterest ui = new UserInterest();
            ui.setUserId(userId);
            ui.setTagId(tagId);
            ui.setWeight(5.0);
            userInterestMapper.insert(ui);
        }
    }
}
