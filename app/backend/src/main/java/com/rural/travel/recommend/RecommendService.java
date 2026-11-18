package com.rural.travel.recommend;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rural.travel.entity.*;
import com.rural.travel.mapper.*;
import com.rural.travel.service.InterestService;
import com.rural.travel.service.ScenicSpotService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 推荐服务:内容特征匹配 + 用户行为相似性 双路融合
 * 前端仅展示自然化文案(为你推荐/猜你喜欢),不出现任何技术名词
 */
@Service
public class RecommendService {

    private static final int TOP_N = 10;

    private final RecommendResultMapper recommendResultMapper;
    private final RatingMapper ratingMapper;
    private final VirtualRatingMapper virtualRatingMapper;
    private final FavoriteMapper favoriteMapper;
    private final TagMapper tagMapper;
    private final CategoryMapper categoryMapper;
    private final ScenicSpotService scenicSpotService;
    private final InterestService interestService;

    public RecommendService(RecommendResultMapper recommendResultMapper, RatingMapper ratingMapper,
                            VirtualRatingMapper virtualRatingMapper, FavoriteMapper favoriteMapper,
                            TagMapper tagMapper, CategoryMapper categoryMapper,
                            ScenicSpotService scenicSpotService, InterestService interestService) {
        this.recommendResultMapper = recommendResultMapper;
        this.ratingMapper = ratingMapper;
        this.virtualRatingMapper = virtualRatingMapper;
        this.favoriteMapper = favoriteMapper;
        this.tagMapper = tagMapper;
        this.categoryMapper = categoryMapper;
        this.scenicSpotService = scenicSpotService;
        this.interestService = interestService;
    }

    /** 获取推荐列表(缓存1小时,过期自动重算);游客返回热门 */
    public List<ScenicSpot> recommendFor(Long userId, int limit) {
        if (userId == null) {
            List<ScenicSpot> hot = scenicSpotService.hot(limit);
            hot.forEach(s -> s.setReason("近期热门，口碑不错"));
            return hot;
        }
        List<RecommendResult> cached = recommendResultMapper.selectList(
                new QueryWrapper<RecommendResult>().eq("user_id", userId)
                        .orderByDesc("score").last("LIMIT " + limit));
        boolean fresh = !cached.isEmpty() && cached.get(0).getCreatedAt() != null
                && cached.get(0).getCreatedAt().isAfter(LocalDateTime.now().minusHours(1));
        if (!fresh) {
            recompute(userId);
            cached = recommendResultMapper.selectList(new QueryWrapper<RecommendResult>()
                    .eq("user_id", userId).orderByDesc("score").last("LIMIT " + limit));
        }
        if (cached.isEmpty()) {
            List<ScenicSpot> hot = scenicSpotService.hot(limit);
            hot.forEach(s -> s.setReason("近期热门，口碑不错"));
            return hot;
        }
        Map<Long, String> reasonMap = cached.stream().collect(Collectors.toMap(
                RecommendResult::getScenicSpotId, r -> r.getReason() == null ? "" : r.getReason(), (a, b) -> a));
        List<ScenicSpot> spots = new ArrayList<>();
        for (RecommendResult r : cached) {
            ScenicSpot s = scenicSpotService.getById(r.getScenicSpotId());
            if (s.getStatus() != null && s.getStatus() == 1) {
                s.setReason(reasonMap.get(s.getId()));
                spots.add(s);
            }
        }
        scenicSpotService.enrich(spots);
        return spots;
    }

    /** 重新计算某用户的推荐结果 */
    public void recompute(Long userId) {
        List<ScenicSpot> candidates = scenicSpotService.allActive();
        if (candidates.isEmpty()) return;

        Map<Long, Double> tagW = interestService.tagWeights(userId);
        Map<Long, Double> catW = interestService.categoryWeights(userId);
        double maxTagW = tagW.values().stream().mapToDouble(d -> d).max().orElse(1.0);
        double maxCatW = catW.values().stream().mapToDouble(d -> d).max().orElse(1.0);

        Map<Long, String> tagNames = tagMapper.selectList(null).stream()
                .collect(Collectors.toMap(Tag::getId, Tag::getName));
        Map<Long, String> catNames = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        // 评分向量:真实用户(id>0) + 虚拟用户(id取负)
        Map<Long, Map<Long, Double>> vectors = new HashMap<>();
        for (Rating r : ratingMapper.selectList(null)) {
            vectors.computeIfAbsent(r.getUserId(), k -> new HashMap<>())
                    .put(r.getScenicSpotId(), r.getScore().doubleValue());
        }
        for (VirtualRating vr : virtualRatingMapper.selectList(null)) {
            vectors.computeIfAbsent(-vr.getVirtualUserId(), k -> new HashMap<>())
                    .put(vr.getScenicSpotId(), vr.getScore().doubleValue());
        }
        Map<Long, Double> myVec = vectors.getOrDefault(userId, Map.of());

        // 与行为相近的游客
        Map<Long, Double> sims = new HashMap<>();
        if (!myVec.isEmpty()) {
            for (Map.Entry<Long, Map<Long, Double>> e : vectors.entrySet()) {
                if (e.getKey().equals(userId)) continue;
                double sim = cosine(myVec, e.getValue());
                if (sim > 0.05) sims.put(e.getKey(), sim);
            }
        }

        List<Scored> scored = new ArrayList<>();
        for (ScenicSpot s : candidates) {
            if (myVec.containsKey(s.getId())) continue; // 已评分的不重复推荐
            // ---- 内容特征分 ----
            double tagScore = 0;
            String bestTag = null;
            double bestW = 0;
            for (Long tagId : ScenicSpotService.parseTagIds(s.getTagIds())) {
                double w = tagW.getOrDefault(tagId, 0.0) / maxTagW;
                tagScore += w;
                if (w > bestW) { bestW = w; bestTag = tagNames.get(tagId); }
            }
            double content = Math.min(tagScore, 1.0) * 0.7;
            double catRaw = catW.getOrDefault(s.getCategoryId(), 0.0);
            if (maxCatW > 0) content += Math.min(catRaw / maxCatW, 1.0) * 0.3;
            double prior = (s.getAvgScore() == null ? 0 : s.getAvgScore().doubleValue()) / 5.0;
            // ---- 行为相似分 ----
            double weighted = 0, simSum = 0;
            for (Map.Entry<Long, Double> e : sims.entrySet()) {
                Double v = vectors.get(e.getKey()).get(s.getId());
                if (v != null) { weighted += e.getValue() * v; simSum += e.getValue(); }
            }
            boolean hasCf = simSum > 0;
            double cf = hasCf ? (weighted / simSum) / 5.0 : 0;
            double finalScore = hasCf ? content * 0.55 + cf * 0.45 : content * 0.75 + prior * 0.25;
            // ---- 自然化推荐语 ----
            String reason;
            if (hasCf && cf >= content) reason = "与你口味相近的游客也喜欢";
            else if (bestTag != null) reason = "与你感兴趣的「" + bestTag + "」相符";
            else if (catRaw > 0) reason = "你喜欢的「" + catNames.getOrDefault(s.getCategoryId(), "同类") + "」类好去处";
            else reason = "近期热门，口碑不错";
            scored.add(new Scored(s.getId(), finalScore, reason));
        }
        scored.sort((a, b) -> Double.compare(b.score, a.score));

        recommendResultMapper.delete(new QueryWrapper<RecommendResult>().eq("user_id", userId));
        scored.stream().limit(TOP_N).forEach(sc -> {
            RecommendResult r = new RecommendResult();
            r.setUserId(userId);
            r.setScenicSpotId(sc.spotId);
            r.setScore(sc.score);
            r.setReason(sc.reason);
            recommendResultMapper.insert(r);
        });
    }

    /** 详情页「猜你喜欢」:同类景区 */
    public List<ScenicSpot> similarSpots(Long spotId, int limit) {
        ScenicSpot base = scenicSpotService.getById(spotId);
        Set<Long> baseTags = new HashSet<>(ScenicSpotService.parseTagIds(base.getTagIds()));
        double basePrice = base.getTicketPrice() == null ? 0 : base.getTicketPrice().doubleValue();
        List<ScenicSpot> result = scenicSpotService.allActive().stream()
                .filter(s -> !s.getId().equals(spotId))
                .map(s -> {
                    Set<Long> tags = new HashSet<>(ScenicSpotService.parseTagIds(s.getTagIds()));
                    tags.retainAll(baseTags);
                    double score = tags.size() * 2.0
                            + (s.getCategoryId().equals(base.getCategoryId()) ? 1.5 : 0)
                            + (s.getAvgScore() == null ? 0 : s.getAvgScore().doubleValue()) / 5.0 * 0.5
                            - Math.abs((s.getTicketPrice() == null ? 0 : s.getTicketPrice().doubleValue()) - basePrice) / 200.0;
                    return new Scored(s.getId(), score, null);
                })
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .limit(limit)
                .map(sc -> {
                    ScenicSpot s = scenicSpotService.getById(sc.spotId);
                    s.setReason("与「" + base.getName() + "」风格相近");
                    return s;
                })
                .collect(Collectors.toList());
        scenicSpotService.enrich(result);
        return result;
    }

    private static double cosine(Map<Long, Double> a, Map<Long, Double> b) {
        double dot = 0, na = 0, nb = 0;
        for (Map.Entry<Long, Double> e : a.entrySet()) {
            na += e.getValue() * e.getValue();
            Double bv = b.get(e.getKey());
            if (bv != null) dot += e.getValue() * bv;
        }
        for (double v : b.values()) nb += v * v;
        if (na == 0 || nb == 0) return 0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    private record Scored(Long spotId, double score, String reason) {}
}
