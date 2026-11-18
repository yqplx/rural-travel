package com.rural.travel.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rural.travel.common.PageResult;
import com.rural.travel.dto.ScenicQueryDTO;
import com.rural.travel.dto.ScenicSaveDTO;
import com.rural.travel.entity.Category;
import com.rural.travel.entity.ScenicImage;
import com.rural.travel.entity.ScenicSpot;
import com.rural.travel.entity.Tag;
import com.rural.travel.exception.BizException;
import com.rural.travel.mapper.CategoryMapper;
import com.rural.travel.mapper.ScenicImageMapper;
import com.rural.travel.mapper.ScenicSpotMapper;
import com.rural.travel.mapper.TagMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScenicSpotService {

    private final ScenicSpotMapper scenicSpotMapper;
    private final ScenicImageMapper scenicImageMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;

    public ScenicSpotService(ScenicSpotMapper scenicSpotMapper, ScenicImageMapper scenicImageMapper,
                             CategoryMapper categoryMapper, TagMapper tagMapper) {
        this.scenicSpotMapper = scenicSpotMapper;
        this.scenicImageMapper = scenicImageMapper;
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
    }

    public PageResult<ScenicSpot> page(ScenicQueryDTO q) {
        QueryWrapper<ScenicSpot> w = new QueryWrapper<ScenicSpot>()
                .eq("status", 1)
                .and(StringUtils.hasText(q.getKeyword()), k -> k.like("name", q.getKeyword())
                        .or().like("description", q.getKeyword())
                        .or().like("features", q.getKeyword())
                        .or().like("city", q.getKeyword()))
                .eq(q.getCategoryId() != null, "category_id", q.getCategoryId())
                .like(q.getTagId() != null, "tag_ids", "," + q.getTagId() + ",")
                .ge(q.getMinPrice() != null, "ticket_price", q.getMinPrice())
                .le(q.getMaxPrice() != null, "ticket_price", q.getMaxPrice());
        String sort = q.getSort() == null ? "" : q.getSort();
        switch (sort) {
            case "price_asc" -> w.orderByAsc("ticket_price");
            case "price_desc" -> w.orderByDesc("ticket_price");
            case "view" -> w.orderByDesc("view_count");
            case "newest" -> w.orderByDesc("id");
            default -> w.orderByDesc("avg_score").orderByDesc("score_count");
        }
        Page<ScenicSpot> page = scenicSpotMapper.selectPage(new Page<>(q.getCurrent(), q.getSize()), w);
        enrich(page.getRecords());
        return PageResult.of(page);
    }

    public ScenicSpot getById(Long id) {
        ScenicSpot spot = scenicSpotMapper.selectById(id);
        if (spot == null) throw new BizException(404, "景区不存在或已下架");
        return spot;
    }

    public ScenicSpot detail(Long id) {
        ScenicSpot spot = getById(id);
        spot.setViewCount(spot.getViewCount() + 1);
        scenicSpotMapper.updateById(spot);
        spot.setImages(scenicImageMapper.selectList(new QueryWrapper<ScenicImage>()
                .eq("scenic_spot_id", id).orderByAsc("sort_order")));
        enrich(List.of(spot));
        return spot;
    }

    /** 填充分类名/标签名 */
    public void enrich(List<ScenicSpot> spots) {
        if (spots.isEmpty()) return;
        Map<Long, String> catMap = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
        Map<Long, String> tagMap = tagMapper.selectList(null).stream()
                .collect(Collectors.toMap(Tag::getId, Tag::getName));
        for (ScenicSpot s : spots) {
            s.setCategoryName(catMap.get(s.getCategoryId()));
            s.setTagNames(parseTagIds(s.getTagIds()).stream().map(tagMap::get)
                    .filter(Objects::nonNull).collect(Collectors.toList()));
        }
    }

    /** tag_ids 以 ",1,2,3," 形式存储便于精确 LIKE 匹配 */
    public static List<Long> parseTagIds(String tagIds) {
        if (!StringUtils.hasText(tagIds)) return List.of();
        return Arrays.stream(tagIds.split(","))
                .filter(StringUtils::hasText).map(Long::valueOf).collect(Collectors.toList());
    }

    public static String wrapTagIds(Collection<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) return null;
        return "," + tagIds.stream().map(String::valueOf).collect(Collectors.joining(",")) + ",";
    }

    public List<ScenicSpot> hot(int limit) {
        List<ScenicSpot> list = scenicSpotMapper.selectList(new QueryWrapper<ScenicSpot>()
                .eq("status", 1)
                .orderByDesc("avg_score").orderByDesc("view_count")
                .last("LIMIT " + limit));
        enrich(list);
        return list;
    }

    public List<ScenicSpot> allActive() {
        return scenicSpotMapper.selectList(new QueryWrapper<ScenicSpot>().eq("status", 1));
    }

    // ==================== 管理端 ====================

    public PageResult<ScenicSpot> adminPage(long current, long size, String keyword, Long categoryId, Integer status) {
        Page<ScenicSpot> page = scenicSpotMapper.selectPage(new Page<>(current, size),
                new QueryWrapper<ScenicSpot>()
                        .like(StringUtils.hasText(keyword), "name", keyword)
                        .eq(categoryId != null, "category_id", categoryId)
                        .eq(status != null, "status", status)
                        .orderByDesc("id"));
        enrich(page.getRecords());
        return PageResult.of(page);
    }

    @Transactional
    public ScenicSpot save(ScenicSaveDTO dto) {
        ScenicSpot s = dto.getId() == null ? new ScenicSpot()
                : Optional.ofNullable(scenicSpotMapper.selectById(dto.getId()))
                        .orElseThrow(() -> new BizException(404, "景区不存在"));
        s.setName(dto.getName());
        s.setCategoryId(dto.getCategoryId());
        s.setTagIds(wrapTagIds(dto.getTagIds()));
        s.setProvince(dto.getProvince());
        s.setCity(dto.getCity());
        s.setAddress(dto.getAddress());
        s.setDescription(dto.getDescription());
        s.setFeatures(dto.getFeatures());
        s.setTips(dto.getTips());
        s.setTicketPrice(dto.getTicketPrice());
        s.setOpenTime(dto.getOpenTime());
        s.setCoverImage(dto.getCoverImage());
        s.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        if (dto.getId() == null) scenicSpotMapper.insert(s); else scenicSpotMapper.updateById(s);
        if (dto.getImages() != null) {
            scenicImageMapper.delete(new QueryWrapper<ScenicImage>().eq("scenic_spot_id", s.getId()));
            int i = 0;
            for (String url : dto.getImages()) {
                if (!StringUtils.hasText(url)) continue;
                ScenicImage img = new ScenicImage();
                img.setScenicSpotId(s.getId());
                img.setImageUrl(url);
                img.setSortOrder(i++);
                scenicImageMapper.insert(img);
            }
        }
        enrich(List.of(s));
        return s;
    }

    @Transactional
    public void delete(Long id) {
        scenicSpotMapper.deleteById(id);
        scenicImageMapper.delete(new QueryWrapper<ScenicImage>().eq("scenic_spot_id", id));
    }

    /** 重新计算景区综合评分(真实评分 + 虚拟样本) */
    public void refreshAvgScore(Long spotId, Double avg, Integer count) {
        ScenicSpot s = scenicSpotMapper.selectById(spotId);
        if (s == null) return;
        s.setAvgScore(java.math.BigDecimal.valueOf(Math.round(avg * 10) / 10.0));
        s.setScoreCount(count);
        scenicSpotMapper.updateById(s);
    }
}
