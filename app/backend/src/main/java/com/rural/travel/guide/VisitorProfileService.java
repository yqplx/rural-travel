package com.rural.travel.guide;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rural.travel.entity.Category;
import com.rural.travel.entity.Tag;
import com.rural.travel.entity.UserInterest;
import com.rural.travel.guide.dto.VisitorProfileDTO;
import com.rural.travel.guide.entity.VisitorProfile;
import com.rural.travel.guide.mapper.VisitorProfileMapper;
import com.rural.travel.mapper.CategoryMapper;
import com.rural.travel.mapper.TagMapper;
import com.rural.travel.mapper.UserInterestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * S3 游客画像：显式设置 + 行为推断。
 * 画像决定式(1) 的偏好匹配度 p_u,i、式(2) 的 T_u 与节奏、
 * 以及式(3) 的基础深度档 k_u。
 */
@Service
@RequiredArgsConstructor
public class VisitorProfileService {

    private static final int INFER_TOP_N = 6;

    private final VisitorProfileMapper profileMapper;
    private final UserInterestMapper userInterestMapper;
    private final TagMapper tagMapper;
    private final CategoryMapper categoryMapper;

    /** 取画像，不存在则按默认值创建 */
    public VisitorProfile getOrCreate(Long userId) {
        VisitorProfile p = profileMapper.selectOne(
                new LambdaQueryWrapper<VisitorProfile>().eq(VisitorProfile::getUserId, userId));
        if (p == null) {
            p = new VisitorProfile();
            p.setUserId(userId);
            p.setAgeBand("青年");
            p.setGroupType("solo");
            p.setPace("normal");
            p.setPreferDialect("普通话");
            p.setDepthLevel(2);
            p.setPlanMinutes(240);
            p.setAvoidCrowd(1);
            p.setInterests("");
            profileMapper.insert(p);
        }
        p.setInterestList(KgService.splitTags(p.getInterests()));
        return p;
    }

    public VisitorProfile save(Long userId, VisitorProfileDTO dto) {
        VisitorProfile p = getOrCreate(userId);
        if (StringUtils.hasText(dto.getAgeBand())) p.setAgeBand(dto.getAgeBand());
        if (StringUtils.hasText(dto.getGroupType())) p.setGroupType(dto.getGroupType());
        if (StringUtils.hasText(dto.getPace())) p.setPace(dto.getPace());
        if (StringUtils.hasText(dto.getPreferDialect())) p.setPreferDialect(dto.getPreferDialect());
        if (dto.getDepthLevel() != null) {
            p.setDepthLevel(Math.max(1, Math.min(3, dto.getDepthLevel())));
        }
        if (dto.getPlanMinutes() != null && dto.getPlanMinutes() > 0) p.setPlanMinutes(dto.getPlanMinutes());
        if (dto.getAvoidCrowd() != null) p.setAvoidCrowd(dto.getAvoidCrowd() == 1 ? 1 : 0);
        if (dto.getInterests() != null) {
            p.setInterests(String.join(",", dto.getInterests()));
            p.setInterestList(dto.getInterests());
        }
        profileMapper.updateById(p);
        p.setInterestList(KgService.splitTags(p.getInterests()));
        return p;
    }

    /** 兴趣集合，供式(1) 计算偏好匹配度 p_u,i */
    public Set<String> interestSet(VisitorProfile p) {
        if (p == null) return Set.of();
        return new HashSet<>(KgService.splitTags(p.getInterests()));
    }

    /**
     * 行为推断：把 user_interest 中权重最高的标签名与分类名写入画像。
     * 标签词表与景区/图谱实体共用，保证 p_u,i 可比。
     */
    public VisitorProfile infer(Long userId) {
        VisitorProfile p = getOrCreate(userId);
        List<UserInterest> interests = userInterestMapper.selectList(
                new LambdaQueryWrapper<UserInterest>().eq(UserInterest::getUserId, userId)
                        .orderByDesc(UserInterest::getWeight));
        if (interests.isEmpty()) return p;

        Map<Long, String> tagNames = tagMapper.selectList(null).stream()
                .collect(Collectors.toMap(Tag::getId, Tag::getName, (a, b) -> a));
        Map<Long, String> catNames = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));

        LinkedHashSet<String> picked = new LinkedHashSet<>();
        for (UserInterest ui : interests) {
            if (ui.getTagId() != null && tagNames.containsKey(ui.getTagId())) {
                picked.add(tagNames.get(ui.getTagId()));
            }
            if (picked.size() >= INFER_TOP_N) break;
        }
        for (UserInterest ui : interests) {
            if (picked.size() >= INFER_TOP_N) break;
            if (ui.getCategoryId() != null && catNames.containsKey(ui.getCategoryId())) {
                picked.add(catNames.get(ui.getCategoryId()));
            }
        }
        if (!picked.isEmpty()) {
            p.setInterests(String.join(",", picked));
            p.setInterestList(new ArrayList<>(picked));
            profileMapper.updateById(p);
        }
        return p;
    }

    /** 画像摘要：前端直接展示的中文标签 */
    public Map<String, Object> summary(VisitorProfile p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ageBand", p.getAgeBand());
        m.put("groupType", groupName(p.getGroupType()));
        m.put("pace", paceName(p.getPace()));
        m.put("interests", p.getInterestList() == null ? List.of() : p.getInterestList());
        m.put("preferDialect", p.getPreferDialect());
        m.put("depthLevel", p.getDepthLevel());
        m.put("depthName", depthName(p.getDepthLevel()));
        m.put("planMinutes", p.getPlanMinutes());
        m.put("avoidCrowd", p.getAvoidCrowd() == 1);
        return m;
    }

    public static String groupName(String g) {
        if (g == null) return "独自出行";
        return switch (g) {
            case "couple" -> "情侣/夫妻";
            case "family" -> "亲子家庭";
            case "friends" -> "朋友结伴";
            case "team" -> "团队出游";
            default -> "独自出行";
        };
    }

    public static String paceName(String pace) {
        if (pace == null) return "适中";
        return switch (pace) {
            case "slow" -> "慢游";
            case "fast" -> "快刷";
            default -> "适中";
        };
    }

    public static String depthName(Integer d) {
        if (d == null) return "标准";
        return switch (d) {
            case 1 -> "简约";
            case 3 -> "深度";
            default -> "标准";
        };
    }
}
