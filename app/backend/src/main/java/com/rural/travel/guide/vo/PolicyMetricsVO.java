package com.rural.travel.guide.vo;

import lombok.Data;

import java.util.List;

/** 政策契合度指标：创新数字内容消费 / 智慧文旅发展 / 乡村振兴数智赋能 */
@Data
public class PolicyMetricsVO {
    // 创新数字内容消费
    private Integer contentTotal;
    private Integer contentAiGenerated;
    private Double aiRatio;
    private Integer modalityCovered;
    private Double contentCoverage;
    /** 按模态的产量分布 */
    private List<NameValue> byModality;
    /** 按风格的产量分布 */
    private List<NameValue> byStyle;
    /** 方言内容数 */
    private Integer dialectCount;

    // 智慧文旅发展
    private Integer guideSessions;
    private Integer routeCount;
    private Double avgRouteNodes;
    private Double satisfaction;
    private Integer feedbackCount;

    // 乡村振兴数智赋能
    private Integer countyCount;
    private Integer entityTotal;
    private Integer heritageCount;
    private Integer foodCount;
    /** 数字内容产量提升（相对人工基线），实施例中为 40% */
    private Double contentEfficiencyGain;
    /** 二次消费转化提升，实施例中为 35% */
    private Double conversionGain;
    /** 游客满意度提升，实施例中为 80% */
    private Double satisfactionGain;

    /** 式(4) 反馈闭环演示：更新前后热度 */
    private List<EmaSample> emaSamples;

    @Data
    public static class NameValue {
        private String name;
        private Long value;

        public NameValue(String name, Long value) {
            this.name = name;
            this.value = value;
        }
    }

    @Data
    public static class EmaSample {
        private String entityName;
        private Double before;
        private Double feedback;
        private Double after;

        public EmaSample(String entityName, Double before, Double feedback, Double after) {
            this.entityName = entityName;
            this.before = before;
            this.feedback = feedback;
            this.after = after;
        }
    }
}
