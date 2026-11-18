package com.rural.travel.ai;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rural.travel.entity.*;
import com.rural.travel.mapper.BrowseHistoryMapper;
import com.rural.travel.mapper.UserMapper;
import com.rural.travel.security.SecurityUtils;
import com.rural.travel.service.ScenicSpotService;
import com.rural.travel.service.UserAiConfigService;
import com.rural.travel.vo.AnalyticsVO;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 智能文旅数据分析:真实聚合 + 7 类规则引擎运营建议 */
@Service
public class AnalyticsService {

    private final BrowseHistoryMapper browseHistoryMapper;
    private final UserMapper userMapper;
    private final ScenicSpotService scenicSpotService;
    private final AiClient aiClient;
    private final UserAiConfigService userAiConfigService;

    public AnalyticsService(BrowseHistoryMapper browseHistoryMapper, UserMapper userMapper,
                            ScenicSpotService scenicSpotService, AiClient aiClient,
                            UserAiConfigService userAiConfigService) {
        this.browseHistoryMapper = browseHistoryMapper;
        this.userMapper = userMapper;
        this.scenicSpotService = scenicSpotService;
        this.aiClient = aiClient;
        this.userAiConfigService = userAiConfigService;
    }

    public AnalyticsVO analyze() {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        List<BrowseHistory> history = browseHistoryMapper.selectList(
                new QueryWrapper<BrowseHistory>().ge("created_at", since));
        Map<Long, ScenicSpot> spots = scenicSpotService.allActive().stream()
                .collect(Collectors.toMap(ScenicSpot::getId, Function.identity()));
        Map<Long, User> users = userMapper.selectList(null).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        AnalyticsVO vo = new AnalyticsVO();

        // ---- 近30天日客流热力(5级) ----
        Map<LocalDate, Integer> daily = new TreeMap<>();
        for (int i = 29; i >= 0; i--) daily.put(LocalDate.now().minusDays(i), 0);
        for (BrowseHistory h : history) {
            LocalDate d = h.getCreatedAt().toLocalDate();
            if (daily.containsKey(d)) daily.merge(d, 1, Integer::sum);
        }
        int maxDaily = daily.values().stream().mapToInt(i -> i).max().orElse(1);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        vo.setDailyHeat(daily.entrySet().stream()
                .map(e -> new AnalyticsVO.DailyHeat(e.getKey().format(fmt), e.getValue(),
                        maxDaily == 0 ? 0 : Math.min(4, e.getValue() * 5 / (maxDaily + 1))))
                .collect(Collectors.toList()));

        // ---- 24小时分布 ----
        List<Integer> hours = new ArrayList<>(Collections.nCopies(24, 0));
        for (BrowseHistory h : history) hours.set(h.getCreatedAt().getHour(),
                hours.get(h.getCreatedAt().getHour()) + 1);
        vo.setHourDist(hours);

        // ---- 分类热度(浏览+评分合并) ----
        Map<Long, Integer> catBrowse = new HashMap<>();
        Map<Long, List<Double>> catScores = new HashMap<>();
        Map<Long, String> catNames = new HashMap<>();
        for (BrowseHistory h : history) {
            ScenicSpot s = spots.get(h.getScenicSpotId());
            if (s == null) continue;
            catBrowse.merge(s.getCategoryId(), 1, Integer::sum);
            if (s.getAvgScore() != null && s.getAvgScore().doubleValue() > 0) {
                catScores.computeIfAbsent(s.getCategoryId(), k -> new ArrayList<>())
                        .add(s.getAvgScore().doubleValue());
            }
        }
        scenicSpotService.enrich(new ArrayList<>(spots.values()));
        for (ScenicSpot s : spots.values()) catNames.put(s.getCategoryId(), s.getCategoryName());
        vo.setCategoryHeat(catBrowse.entrySet().stream()
                .map(e -> new AnalyticsVO.CategoryHeat(
                        catNames.getOrDefault(e.getKey(), "分类" + e.getKey()),
                        e.getValue(),
                        catScores.getOrDefault(e.getKey(), List.of()).stream()
                                .mapToDouble(d -> d).average().orElse(0)))
                .sorted((a, b) -> b.getBrowse() - a.getBrowse())
                .collect(Collectors.toList()));

        // ---- 客源省份 Top10 ----
        Map<String, Integer> province = new HashMap<>();
        for (BrowseHistory h : history) {
            User u = users.get(h.getUserId());
            if (u != null && u.getProvince() != null && !u.getProvince().isBlank()) {
                province.merge(u.getProvince(), 1, Integer::sum);
            }
        }
        vo.setProvinceTop(province.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue()).limit(10)
                .map(e -> Map.<String, Object>of("province", e.getKey(), "count", e.getValue()))
                .collect(Collectors.toList()));

        // ---- 价格偏好 ----
        Map<String, Integer> price = new LinkedHashMap<>();
        price.put("免费", 0); price.put("1-50元", 0); price.put("51-100元", 0); price.put("100元以上", 0);
        for (BrowseHistory h : history) {
            ScenicSpot s = spots.get(h.getScenicSpotId());
            if (s == null || s.getTicketPrice() == null) continue;
            double p = s.getTicketPrice().doubleValue();
            String bucket = p <= 0 ? "免费" : p <= 50 ? "1-50元" : p <= 100 ? "51-100元" : "100元以上";
            price.merge(bucket, 1, Integer::sum);
        }
        vo.setPriceDist(price.entrySet().stream()
                .map(e -> Map.<String, Object>of("label", e.getKey(), "count", e.getValue()))
                .collect(Collectors.toList()));

        // ---- 标签热度 Top10 ----
        Map<Long, Integer> tagCount = new HashMap<>();
        for (BrowseHistory h : history) {
            ScenicSpot s = spots.get(h.getScenicSpotId());
            if (s == null) continue;
            for (Long tagId : ScenicSpotService.parseTagIds(s.getTagIds())) {
                tagCount.merge(tagId, 1, Integer::sum);
            }
        }
        Map<Long, String> tagNames = new HashMap<>();
        for (ScenicSpot s : spots.values()) {
            List<Long> ids = ScenicSpotService.parseTagIds(s.getTagIds());
            List<String> names = s.getTagNames() == null ? List.of() : s.getTagNames();
            for (int i = 0; i < ids.size() && i < names.size(); i++) tagNames.put(ids.get(i), names.get(i));
        }
        vo.setTagHeat(tagCount.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue()).limit(10)
                .map(e -> Map.<String, Object>of("name",
                        tagNames.getOrDefault(e.getKey(), "标签" + e.getKey()), "count", e.getValue()))
                .collect(Collectors.toList()));

        // ---- 周末占比 ----
        long weekend = history.stream()
                .filter(h -> { DayOfWeek d = h.getCreatedAt().getDayOfWeek();
                    return d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY; }).count();
        vo.setWeekendRatio(history.isEmpty() ? 0.0
                : Math.round(weekend * 1000.0 / history.size()) / 10.0);

        vo.setSuggestions(buildSuggestions(vo));
        vo.setSummary(buildSummary(vo, history.size()));
        return vo;
    }

    private List<AnalyticsVO.Suggestion> buildSuggestions(AnalyticsVO vo) {
        List<AnalyticsVO.Suggestion> list = new ArrayList<>();
        // 1 高峰时段
        List<Integer> hours = vo.getHourDist();
        int peak = 0;
        for (int i = 1; i < 24; i++) if (hours.get(i) > hours.get(peak)) peak = i;
        list.add(new AnalyticsVO.Suggestion("高峰时段调度",
                "客流高峰集中在 " + peak + ":00-" + ((peak + 2) % 24) + ":00,建议在该时段增派引导人员、"
                        + "加密接驳班次,并在低谷时段推出限时优惠平衡客流。"));
        // 2 周末策略
        list.add(new AnalyticsVO.Suggestion("周末/工作日策略",
                vo.getWeekendRatio() >= 35
                        ? "周末客流占比达 " + vo.getWeekendRatio() + "%,建议周末实行预约分流与弹性定价,工作日主打团建、研学套餐填补空档。"
                        : "工作日客流相对充足,建议周末增加主题活动(市集、演出)进一步拉升周末热度。"));
        // 3 品类供需
        if (!vo.getCategoryHeat().isEmpty()) {
            AnalyticsVO.CategoryHeat top = vo.getCategoryHeat().get(0);
            int total = vo.getCategoryHeat().stream().mapToInt(AnalyticsVO.CategoryHeat::getBrowse).sum();
            int share = total == 0 ? 0 : top.getBrowse() * 100 / total;
            list.add(new AnalyticsVO.Suggestion("品类供需优化",
                    "「" + top.getName() + "」类热度占比 " + share + "%。"
                            + (share > 40 ? "头部品类集中度过高,建议扶持长尾品类(组合联票、主题线路)分散风险。"
                                          : "品类结构较均衡,可持续观察长尾品类并择机打造新爆款。")));
        }
        // 4 价格带
        vo.getPriceDist().stream().max(Comparator.comparingInt(m -> (Integer) m.get("count")))
                .ifPresent(m -> list.add(new AnalyticsVO.Suggestion("价格带运营",
                        "游客最偏好「" + m.get("label") + "」价位,建议主推该价格带产品,"
                                + "高价产品打包增值服务(餐饮/体验)提升接受度。")));
        // 5 客源地域
        if (!vo.getProvinceTop().isEmpty()) {
            Object topP = vo.getProvinceTop().get(0).get("province");
            list.add(new AnalyticsVO.Suggestion("客源地域投放",
                    "最大客源地是「" + topP + "」,建议在该省份的短视频/社交平台加大投放,"
                            + "并针对第二、三客源地做联动促销。"));
        }
        // 6 标签聚焦
        if (!vo.getTagHeat().isEmpty()) {
            list.add(new AnalyticsVO.Suggestion("主题标签聚焦",
                    "「" + vo.getTagHeat().get(0).get("name") + "」主题最受关注,建议围绕该主题策划内容营销与节庆活动,形成记忆点。"));
        }
        // 7 口碑提升
        vo.getCategoryHeat().stream()
                .filter(c -> c.getRating() > 0)
                .min(Comparator.comparingDouble(AnalyticsVO.CategoryHeat::getRating))
                .ifPresent(c -> list.add(new AnalyticsVO.Suggestion("口碑提升",
                        "「" + c.getName() + "」类综合评分相对较低(" + String.format("%.1f", c.getRating())
                                + "分),建议排查该品类景区的服务与设施短板,优先整改差评集中项。")));
        return list;
    }

    private String buildSummary(AnalyticsVO vo, int total) {
        String base = "近30天全站共产生 " + total + " 次浏览,周末客流占比 " + vo.getWeekendRatio()
                + "%,整体热度" + (total > 200 ? "较高,建议抓住当前流量窗口加大转化。" : "平稳,建议加强引流活动。");
        // 使用当前管理员自己的密钥润色,未配置则用本地规则结论
        Long userId = SecurityUtils.currentUserIdOrNull();
        UserAiConfigService.AiKey key = userAiConfigService.keyOf(userId);
        if (key != null) {
            String polished = aiClient.chatWith(key.apiKey(), key.baseUrl(), key.model(),
                    "你是文旅运营专家,把以下数据分析结论压缩成一句50字以内的运营决策摘要,语气专业果断。",
                    base);
            if (polished != null) return polished;
        }
        return base;
    }
}
