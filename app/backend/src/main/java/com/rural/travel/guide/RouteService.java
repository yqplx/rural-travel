package com.rural.travel.guide;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rural.travel.guide.dto.RoutePlanDTO;
import com.rural.travel.guide.entity.GuideRoute;
import com.rural.travel.guide.entity.GuideRouteItem;
import com.rural.travel.guide.entity.KgEntity;
import com.rural.travel.guide.entity.VisitorProfile;
import com.rural.travel.guide.mapper.GuideRouteItemMapper;
import com.rural.travel.guide.mapper.GuideRouteMapper;
import com.rural.travel.guide.vo.RoutePlanVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * S4 个性化游览路径规划。
 *
 * <p>先在全部候选实体上按式(1) 计算入选分，再以"边际收益 − 接驳时间成本"做贪心扩展，
 * 每步用式(2) 校验时长可行性，从而得到一条时长不超预算、且尽可能贴合画像的路线。</p>
 *
 * <pre>
 * 式(1) s_u,i = α·p_u,i + β·h_i − γ·c_i + δ·g_i
 * 式(2) Σ_{i∈R} w_i + Σ_{(i,j)∈R} t_i,j ≤ T_u
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class RouteService {

    /** 接驳时间成本系数：每小时接驳折算掉的边际收益 */
    private static final double MU = 0.15;

    /** 避堵硬阈值 τ：c_i 超过此值直接不入选（交底书实施例取 0.75） */
    private static final double CROWD_LIMIT = GuideFormulas.CROWD_TAU;

    private final KgService kgService;
    private final VisitorProfileService profileService;
    private final GuideRouteMapper routeMapper;
    private final GuideRouteItemMapper itemMapper;

    /** 规划路线：登录用户落库，游客只算不存 */
    @Transactional
    public RoutePlanVO plan(Long userId, RoutePlanDTO dto) {
        VisitorProfile profile = userId == null ? guestProfile() : profileService.getOrCreate(userId);
        String countyCode = StringUtils.hasText(dto.getCountyCode()) ? dto.getCountyCode() : kgService.defaultCounty();
        int budget = dto.getPlanMinutes() != null && dto.getPlanMinutes() > 0
                ? dto.getPlanMinutes()
                : (profile.getPlanMinutes() == null ? 240 : profile.getPlanMinutes());
        boolean avoidCrowd = dto.getAvoidCrowd() == null ? profile.getAvoidCrowd() == 1 : dto.getAvoidCrowd() == 1;
        int maxItems = dto.getMaxItems() == null || dto.getMaxItems() <= 0 ? 8 : dto.getMaxItems();

        List<KgEntity> candidates = kgService.listRoutable(countyCode, dto.getEntityTypes());
        Set<String> interests = profileService.interestSet(profile);
        Map<Long, Map<Long, Integer>> transferIdx = kgService.transferIndex(countyCode);

        // ---- 式(1)：逐实体打分 ----
        Map<Long, Score> scores = new HashMap<>();
        for (KgEntity e : candidates) {
            Set<String> tags = new HashSet<>(KgService.splitTags(e.getTags()));
            double p = GuideFormulas.preference(interests, tags);
            double h = nz(e.getHeat());
            double c = nz(e.getCrowd());
            double g = nz(e.getContentScore());
            double s = GuideFormulas.routeScore(p, h, c, g);
            if (avoidCrowd && c > CROWD_LIMIT) {
                // 避堵模式下拥挤度过高的节点不入选（式(1) 的 −γ·c_i 之外的硬约束）
                continue;
            }
            scores.put(e.getId(), new Score(s, p, h, c, g));
        }

        // ---- 贪心扩展，每步用式(2) 校验 ----
        List<KgEntity> picked = new ArrayList<>();
        List<Integer> transferList = new ArrayList<>();
        Set<Long> used = new LinkedHashSet<>();

        KgEntity first = pickFirst(candidates, scores, dto.getStartEntityId());
        if (first != null) {
            picked.add(first);
            used.add(first.getId());
        }

        int totalMinutes = 0;
        if (!picked.isEmpty()) {
            totalMinutes += stayOf(first, profile);
        }

        while (picked.size() < maxItems) {
            KgEntity last = picked.get(picked.size() - 1);
            KgEntity best = null;
            int bestTransfer = 0;
            double bestMarginal = Double.NEGATIVE_INFINITY;

            for (KgEntity e : candidates) {
                if (used.contains(e.getId())) continue;
                Score sc = scores.get(e.getId());
                if (sc == null) continue;
                int transfer = transferBetween(transferIdx, last, e, profile);
                int stay = stayOf(e, profile);
                // 式(2)：加入该节点后总时长不得超预算
                if (totalMinutes + transfer + stay > budget) continue;
                double marginal = sc.s - MU * (transfer / 60.0);
                if (marginal > bestMarginal) {
                    bestMarginal = marginal;
                    best = e;
                    bestTransfer = transfer;
                }
            }
            if (best == null) break;   // 预算用完或已无可行节点
            transferList.add(bestTransfer);
            picked.add(best);
            used.add(best.getId());
            totalMinutes += bestTransfer + stayOf(best, profile);
        }

        // ---- 组装结果 ----
        RoutePlanVO vo = new RoutePlanVO();
        vo.setCountyCode(countyCode);
        vo.setRouteName(buildRouteName(profile, picked.size(), budget));
        vo.setPlanMinutes(budget);
        vo.setTotalMinutes(totalMinutes);
        vo.setFeasible(totalMinutes <= budget);
        vo.setRemainMinutes(Math.max(0, budget - totalMinutes));

        List<RoutePlanVO.RouteNodeVO> nodes = new ArrayList<>();
        int clock = 0;
        double sum = 0;
        for (int i = 0; i < picked.size(); i++) {
            KgEntity e = picked.get(i);
            Score sc = scores.get(e.getId());
            int stay = stayOf(e, profile);
            int transfer = i == 0 ? 0 : transferList.get(i - 1);
            clock += transfer;

            RoutePlanVO.RouteNodeVO n = new RoutePlanVO.RouteNodeVO();
            n.setSeq(i + 1);
            n.setEntityId(e.getId());
            n.setEntityName(e.getName());
            n.setEntityType(e.getEntityType());
            n.setCoverImage(e.getCoverImage());
            n.setLng(e.getLng());
            n.setLat(e.getLat());
            n.setStayMinutes(stay);
            n.setTransferMin(transfer);
            n.setArriveMinute(clock);
            n.setScore(round3(sc == null ? 0 : sc.s));
            n.setScorePreference(round3(sc == null ? 0 : sc.p));
            n.setScoreHeat(round3(sc == null ? 0 : sc.h));
            n.setScoreCrowd(round3(sc == null ? 0 : sc.c));
            n.setScoreContent(round3(sc == null ? 0 : sc.g));
            n.setReason(reasonOf(sc, e, stay, transfer, i));
            nodes.add(n);

            clock += stay;
            sum += (sc == null ? 0 : sc.s);
        }
        vo.setNodes(nodes);
        vo.setTotalScore(round3(sum));

        RoutePlanVO.FormulaExplain fe = new RoutePlanVO.FormulaExplain();
        fe.setScoreFormula("s(u,i) = α·p(u,i) + β·h(i) − γ·c(i) + δ·g(i)");
        fe.setTimeFormula("Σ w(i) + Σ t(i,j) ≤ T(u)");
        fe.setAlpha(GuideFormulas.ALPHA);
        fe.setBeta(GuideFormulas.BETA);
        fe.setGamma(GuideFormulas.GAMMA);
        fe.setDelta(GuideFormulas.DELTA);
        fe.setNote("四项权重非负且和为 1；拥挤度取负号体现避堵；接驳时长优先取图谱 TRANSFER/NEARBY 关系，缺省按步行 80 米/分钟估算。");
        vo.setFormula(fe);

        if (userId != null) {
            GuideRoute route = persist(userId, countyCode, budget, totalMinutes, sum, nodes, avoidCrowd);
            vo.setRouteId(route.getId());
        }
        return vo;
    }

    private GuideRoute persist(Long userId, String countyCode, int budget, int totalMinutes,
                               double totalScore, List<RoutePlanVO.RouteNodeVO> nodes, boolean avoidCrowd) {
        GuideRoute route = new GuideRoute();
        route.setUserId(userId);
        route.setCountyCode(countyCode);
        route.setPlanMinutes(budget);
        route.setTotalMinutes(totalMinutes);
        route.setTotalScore(round3(totalScore));
        route.setItemCount(nodes.size());
        route.setAvoidCrowd(avoidCrowd ? 1 : 0);
        route.setStatus("ACTIVE");
        routeMapper.insert(route);

        for (RoutePlanVO.RouteNodeVO n : nodes) {
            GuideRouteItem item = new GuideRouteItem();
            item.setRouteId(route.getId());
            item.setEntityId(n.getEntityId());
            item.setSeq(n.getSeq());
            item.setArriveMinute(n.getArriveMinute());
            item.setStayMinutes(n.getStayMinutes());
            item.setTransferMin(n.getTransferMin());
            item.setScore(n.getScore());
            item.setReason(n.getReason());
            itemMapper.insert(item);
        }
        return route;
    }

    public List<GuideRoute> myRoutes(Long userId) {
        return routeMapper.selectList(new LambdaQueryWrapper<GuideRoute>()
                .eq(GuideRoute::getUserId, userId)
                .orderByDesc(GuideRoute::getCreatedAt));
    }

    public RoutePlanVO routeDetail(Long routeId) {
        GuideRoute route = routeMapper.selectById(routeId);
        if (route == null) return null;
        List<GuideRouteItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<GuideRouteItem>().eq(GuideRouteItem::getRouteId, routeId)
                        .orderByAsc(GuideRouteItem::getSeq));
        for (GuideRouteItem it : items) {
            KgEntity e = kgService.get(it.getEntityId());
            if (e != null) {
                it.setEntityName(e.getName());
                it.setEntityType(e.getEntityType());
                it.setCoverImage(e.getCoverImage());
                it.setLng(e.getLng());
                it.setLat(e.getLat());
            }
        }
        RoutePlanVO vo = new RoutePlanVO();
        vo.setRouteId(route.getId());
        vo.setCountyCode(route.getCountyCode());
        vo.setRouteName(route.getName());
        vo.setPlanMinutes(route.getPlanMinutes());
        vo.setTotalMinutes(route.getTotalMinutes());
        vo.setFeasible(route.getTotalMinutes() <= route.getPlanMinutes());
        vo.setRemainMinutes(Math.max(0, route.getPlanMinutes() - route.getTotalMinutes()));
        vo.setTotalScore(route.getTotalScore());
        route.setItems(items);
        List<RoutePlanVO.RouteNodeVO> nodes = items.stream().map(it -> {
            RoutePlanVO.RouteNodeVO n = new RoutePlanVO.RouteNodeVO();
            n.setSeq(it.getSeq());
            n.setEntityId(it.getEntityId());
            n.setEntityName(it.getEntityName());
            n.setEntityType(it.getEntityType());
            n.setCoverImage(it.getCoverImage());
            n.setLng(it.getLng());
            n.setLat(it.getLat());
            n.setStayMinutes(it.getStayMinutes());
            n.setTransferMin(it.getTransferMin());
            n.setArriveMinute(it.getArriveMinute());
            n.setScore(it.getScore());
            n.setReason(it.getReason());
            return n;
        }).toList();
        vo.setNodes(nodes);
        return vo;
    }

    // ---------------- 内部计算 ----------------

    private KgEntity pickFirst(List<KgEntity> candidates, Map<Long, Score> scores, Long startEntityId) {
        if (startEntityId != null) {
            for (KgEntity e : candidates) {
                if (startEntityId.equals(e.getId()) && scores.containsKey(e.getId())) return e;
            }
        }
        return candidates.stream().filter(e -> scores.containsKey(e.getId()))
                .max(Comparator.comparingDouble(e -> scores.get(e.getId()).s)).orElse(null);
    }

    /** 停留时长 w_i：建议时长按画像节奏缩放 */
    private int stayOf(KgEntity e, VisitorProfile profile) {
        int base = e.getVisitMinutes() == null || e.getVisitMinutes() <= 0 ? 30 : e.getVisitMinutes();
        return (int) Math.round(base * GuideFormulas.paceStayFactor(profile.getPace()));
    }

    /** 接驳时长 t_ij：优先取图谱关系，缺省按直线距离与步行速度估算 */
    private int transferBetween(Map<Long, Map<Long, Integer>> idx, KgEntity from, KgEntity to, VisitorProfile profile) {
        Map<Long, Integer> m = idx.get(from.getId());
        if (m != null && m.containsKey(to.getId())) return m.get(to.getId());
        if (from.getLng() == null || from.getLat() == null || to.getLng() == null || to.getLat() == null) {
            return 10;
        }
        double meters = GuideFormulas.distanceMeters(from.getLng(), from.getLat(), to.getLng(), to.getLat());
        return GuideFormulas.walkMinutes(meters, profile.getPace());
    }

    private String reasonOf(Score sc, KgEntity e, int stay, int transfer, int index) {
        if (sc == null) return "备选节点";
        List<String> parts = new ArrayList<>();
        if (sc.p >= 0.6) parts.add("高度契合你的" + firstTag(e) + "偏好");
        else if (sc.p > 0) parts.add("部分契合你的兴趣");
        else parts.add("县域必打卡");
        if (sc.h >= 0.6) parts.add("热度靠前");
        if (sc.c <= 0.35) parts.add("当前不拥挤");
        else if (sc.c >= 0.6) parts.add("人流较多,建议错峰");
        if (sc.g >= 0.75) parts.add("图文语音视频内容齐全");
        else if (sc.g <= 0.25) parts.add("内容仍在补齐中");
        String head = index == 0 ? "【起点】" : "【第" + (index + 1) + "站】";
        return head + String.join("，", parts) + "；建议停留 " + stay + " 分钟"
                + (transfer > 0 ? "，步行接驳约 " + transfer + " 分钟" : "");
    }

    private String firstTag(KgEntity e) {
        List<String> tags = KgService.splitTags(e.getTags());
        return tags.isEmpty() ? "县域" : tags.get(0);
    }

    private String buildRouteName(VisitorProfile profile, int count, int budget) {
        return VisitorProfileService.paceName(profile.getPace()) + count + "站 · " + budget + "分钟"
                + (profile.getAvoidCrowd() == 1 ? " · 避堵" : "");
    }

    private VisitorProfile guestProfile() {
        VisitorProfile p = new VisitorProfile();
        p.setPace("normal");
        p.setDepthLevel(2);
        p.setPlanMinutes(240);
        p.setAvoidCrowd(1);
        p.setInterests("");
        p.setInterestList(List.of());
        return p;
    }

    private static double nz(Double v) {
        return v == null ? 0d : v;
    }

    private static double round3(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }

    /** 式(1) 的五项分量 */
    private record Score(double s, double p, double h, double c, double g) {
    }
}
