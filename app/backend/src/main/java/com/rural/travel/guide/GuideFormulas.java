package com.rural.travel.guide;

import java.util.List;
import java.util.Set;

/**
 * 方案三 算法内核：与专利交底书 式(1)~式(4) 一一对应的纯函数。
 *
 * <p>本类只做数值计算，不依赖 Spring、不访问数据库、不接触 HTTP，
 * 便于单元测试与参数敏感性分析。所有系数集中在此，作为唯一事实来源。</p>
 *
 * <pre>
 * 式(1) s_u,i = α·p_u,i + β·h_i − γ·c_i + δ·g_i
 * 式(2) Σ_{i∈R} w_i + Σ_{(i,j)∈R} t_i,j ≤ T_u，且各景点 c_i ≤ τ
 * 式(3) d_u = ⌊θ1·k_u + θ2·r_u + θ3·q_u + 1⌋，且 d_u ≤ 3
 * 式(4) h_i^(t+1) = (1 − η)·h_i^(t) + η·f_i^(t)
 * </pre>
 *
 * <p>式(3) 三个自变量的语义严格遵循交底书 3.4 符号表：
 * k_u 为知识背景水平、r_u 为互动参与度、q_u 为同行人员构成因子，三者取值均为 [0,3]。</p>
 */
public final class GuideFormulas {

    private GuideFormulas() {
    }

    // ---- 式(1) 权重：四项非负且和为 1 ----
    public static final double ALPHA = 0.40;   // 偏好匹配
    public static final double BETA = 0.25;    // 热度
    public static final double GAMMA = 0.20;   // 拥挤度（取负号体现"避堵"）
    public static final double DELTA = 0.15;   // 内容完备度

    // ---- 式(3) 系数（与交底书 3.5 参数表实施例取值一致）----
    public static final double THETA1 = 0.30;  // 知识背景水平 k_u
    public static final double THETA2 = 0.25;  // 互动参与度 r_u
    public static final double THETA3 = 0.20;  // 同行人员构成因子 q_u
    public static final int DEPTH_MAX = 3;     // 档位上限 1简约 2标准 3深度
    /** 自变量上界，交底书符号表规定 k_u / r_u / q_u ∈ [0,3] */
    public static final double DEPTH_INPUT_MAX = 3.0;

    // ---- 式(4) 平滑因子 ----
    public static final double ETA = 0.20;

    /** 式(2) 拥挤度上限阈值 τ：超过该值的景点不纳入路线（交底书实施例取 0.75） */
    public static final double CROWD_TAU = 0.75;

    /** 内容完备度 g_i 的满分所需模态数：文案/语音稿/图像/视频脚本 */
    public static final int MODALITY_TARGET = 4;

    /** 讲解触发半径（米） */
    public static final int TRIGGER_RADIUS_M = 120;

    /** 步行速度（米/分钟），用于无接驳关系时估算 t_ij */
    public static final double WALK_SPEED_MPM = 80.0;

    /** 地球平均半径（米） */
    private static final double EARTH_RADIUS_M = 6_371_000.0;

    /**
     * 式(1) 景点入选分。
     *
     * @param p 偏好匹配度 p_u,i ∈ [0,1]
     * @param h 热度 h_i ∈ [0,1]
     * @param c 拥挤度 c_i ∈ [0,1]，取负号
     * @param g 内容完备度 g_i ∈ [0,1]
     */
    public static double routeScore(double p, double h, double c, double g) {
        return ALPHA * clip01(p) + BETA * clip01(h) - GAMMA * clip01(c) + DELTA * clip01(g);
    }

    /** 式(1) 重载：直接吃实体字段 */
    public static double routeScore(double p, double heat, double crowd, double contentScore, boolean unused) {
        return routeScore(p, heat, crowd, contentScore);
    }

    /**
     * 式(2) 路线时长可行性：游览时长与步行接驳时长之和不超过计划时长。
     *
     * @param stayMinutes     各节点停留时长 w_i
     * @param transferMinutes 相邻节点间接驳时长 t_i,j，长度为 stayMinutes.size()-1
     * @param budgetMinutes   计划游览时长 T_u
     */
    public static boolean timeFeasible(List<Integer> stayMinutes, List<Integer> transferMinutes, int budgetMinutes) {
        long total = 0;
        if (stayMinutes != null) {
            for (Integer w : stayMinutes) total += (w == null ? 0 : w);
        }
        if (transferMinutes != null) {
            for (Integer t : transferMinutes) total += (t == null ? 0 : t);
        }
        return total <= budgetMinutes;
    }

    /** 式(2) 左式：总耗时 */
    public static int totalMinutes(List<Integer> stayMinutes, List<Integer> transferMinutes) {
        long total = 0;
        if (stayMinutes != null) for (Integer w : stayMinutes) total += (w == null ? 0 : w);
        if (transferMinutes != null) for (Integer t : transferMinutes) total += (t == null ? 0 : t);
        return (int) total;
    }

    /**
     * 式(3) 讲解深度档位 d_u = ⌊θ1·k_u + θ2·r_u + θ3·q_u + 1⌋，且 d_u ≤ 3。
     *
     * @param k 知识背景水平 k_u ∈ [0,3]，来自游客画像
     * @param r 互动参与度 r_u ∈ [0,3]，见 {@link #interactionDegree}
     * @param q 同行人员构成因子 q_u ∈ [0,3]，见 {@link #groupFactor}
     * @return 1 简约 / 2 标准 / 3 深度
     */
    public static int guideDepth(double k, double r, double q) {
        double raw = THETA1 * clip(k) + THETA2 * clip(r) + THETA3 * clip(q) + 1;
        int d = (int) Math.floor(raw);
        if (d < 1) d = 1;
        if (d > DEPTH_MAX) d = DEPTH_MAX;   // 上限截断写在公式外
        return d;
    }

    /**
     * 式(4) 反馈驱动的指数平滑更新。
     *
     * @param h 上一轮热度/满意度 h_i^(t)
     * @param f 本轮反馈归一化值 f_i^(t) ∈ [0,1]
     */
    public static double nextEma(double h, double f) {
        return (1 - ETA) * clip01(h) + ETA * clip01(f);
    }

    /** 互动参与度基准值：游客被触发即视为已有基本参与 */
    private static final double INTERACTION_BASE = 1.0;
    /** 停留满建议时长可获得的互动加分上限 */
    private static final double INTERACTION_DWELL_GAIN = 1.2;
    /** 每次追问的互动加分 */
    private static final double INTERACTION_ASK_GAIN = 0.4;

    /**
     * 式(3) 的 r_u 互动参与度 ∈ [0,3]。
     *
     * <p>由"相对停留时长"与"追问次数"两项合成：停留时间相对建议时长的比例越高、追问越多，
     * 说明游客参与越投入，讲解随之加深。刚被触发时取基准值 1.0，
     * 停留满建议时长且追问 2 次可达上界 3.0。</p>
     *
     * @param dwellSeconds       本实体已停留秒数
     * @param recommendedMinutes 该实体建议游览时长 w_i
     * @param askCount           本次会话已追问次数
     */
    public static double interactionDegree(int dwellSeconds, int recommendedMinutes, int askCount) {
        double base = recommendedMinutes <= 0 ? 30.0 : recommendedMinutes;
        double dwellRatio = Math.min(1.5, Math.max(0, dwellSeconds) / 60.0 / base);
        double r = INTERACTION_BASE
                + INTERACTION_DWELL_GAIN * Math.min(1.0, dwellRatio)
                + INTERACTION_ASK_GAIN * Math.max(0, askCount);
        return clip(r);
    }

    /**
     * 式(3) 的 q_u 同行人员构成因子 ∈ [0,3]。
     *
     * <p>反映同行构成对"深度讲解"的接受程度：亲子家庭需要浅显有趣，取值最低；
     * 独行游客时间自由、更愿意听深，取值最高。</p>
     */
    public static double groupFactor(String groupType) {
        if (groupType == null) return 2.0;
        return switch (groupType) {
            case "family" -> 1.0;   // 亲子：浅显有趣优先
            case "team" -> 1.5;     // 团队：统一节奏，不宜过深
            case "couple" -> 2.0;
            case "friends" -> 2.0;
            case "solo" -> 2.5;     // 独行：自由度高，可深讲
            default -> 2.0;
        };
    }

    private static double clip(double v) {
        if (Double.isNaN(v)) return 0.0;
        return Math.max(0.0, Math.min(DEPTH_INPUT_MAX, v));
    }

    /** 偏好匹配度 p_u,i：兴趣标签与实体标签的交集占比 */
    public static double preference(Set<String> interests, Set<String> tags) {
        if (tags == null || tags.isEmpty()) return 0.0;
        if (interests == null || interests.isEmpty()) return 0.0;
        long hit = tags.stream().filter(interests::contains).count();
        return clip01((double) hit / tags.size());
    }

    /** 内容完备度 g_i：已覆盖模态数 / 目标模态数 */
    public static double contentCompleteness(int coveredModalities) {
        return clip01((double) coveredModalities / MODALITY_TARGET);
    }

    /** Haversine 球面距离（米） */
    public static double distanceMeters(double lng1, double lat1, double lng2, double lat2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * EARTH_RADIUS_M * Math.asin(Math.sqrt(a));
    }

    /** 步行接驳时长 t_i,j（分钟），向上取整，pace 影响步行速度 */
    public static int walkMinutes(double meters, String pace) {
        double speed = WALK_SPEED_MPM * paceFactor(pace);
        return (int) Math.ceil(Math.max(0, meters) / speed);
    }

    /** 节奏系数：慢 0.8 / 适中 1.0 / 快 1.25 */
    public static double paceFactor(String pace) {
        if (pace == null) return 1.0;
        return switch (pace) {
            case "slow" -> 0.8;
            case "fast" -> 1.25;
            default -> 1.0;
        };
    }

    /** 节奏对停留时长的缩放：慢 1.3 / 适中 1.0 / 快 0.75 */
    public static double paceStayFactor(String pace) {
        if (pace == null) return 1.0;
        return switch (pace) {
            case "slow" -> 1.3;
            case "fast" -> 0.75;
            default -> 1.0;
        };
    }

    private static double clip01(double v) {
        if (Double.isNaN(v)) return 0.0;
        return Math.max(0.0, Math.min(1.0, v));
    }
}
