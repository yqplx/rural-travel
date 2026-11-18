package com.rural.travel.guide.vo;

import lombok.Data;

import java.util.List;

/** S4 路线规划结果：含式(1)(2) 的可解释明细 */
@Data
public class RoutePlanVO {
    private Long routeId;
    private String countyCode;
    private String routeName;
    /** T_u */
    private Integer planMinutes;
    /** Σw_i + Σt_ij */
    private Integer totalMinutes;
    /** 式(2) 是否满足 */
    private Boolean feasible;
    private Integer remainMinutes;
    private Double totalScore;
    private List<RouteNodeVO> nodes;
    /** 公式口径说明，前端可直接展示 */
    private FormulaExplain formula;

    @Data
    public static class RouteNodeVO {
        private Integer seq;
        private Long entityId;
        private String entityName;
        private String entityType;
        private String coverImage;
        private Double lng;
        private Double lat;
        /** w_i 本节点停留 */
        private Integer stayMinutes;
        /** t_ij 接驳 */
        private Integer transferMin;
        private Integer arriveMinute;
        /** 式(1) 总分 */
        private Double score;
        /** 式(1) 四项分量，用于"为什么推荐它" */
        private Double scorePreference;
        private Double scoreHeat;
        private Double scoreCrowd;
        private Double scoreContent;
        private String reason;
    }

    @Data
    public static class FormulaExplain {
        private String scoreFormula;
        private String timeFormula;
        private Double alpha;
        private Double beta;
        private Double gamma;
        private Double delta;
        private String note;
    }
}
