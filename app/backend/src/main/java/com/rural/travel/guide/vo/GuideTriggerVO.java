package com.rural.travel.guide.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/** S5 位置触发自适应讲解结果 */
@Data
public class GuideTriggerVO {
    private Long sessionId;
    private Long entityId;
    private String entityName;
    private String entityType;
    private String coverImage;
    private Double lng;
    private Double lat;
    /** 触发距离（米） */
    private Integer distanceM;
    /** 是否在触发半径内 */
    private Boolean triggered;
    /** 式(3) 判定出的深度档 d_u */
    private Integer depthLevel;
    private String depthName;
    /** 式(3) 判定明细 */
    private String depthDetail;
    // 单字母前缀字段会被 Jackson legacy 命名规则压成全小写(kvalue)，此处显式声明保持驼峰
    /** k_u 知识背景水平 ∈ [0,3] */
    @JsonProperty("kValue")
    private Double kValue;
    /** r_u 互动参与度 ∈ [0,3] */
    @JsonProperty("rValue")
    private Double rValue;
    /** q_u 同行人员构成因子 ∈ [0,3] */
    @JsonProperty("qValue")
    private Double qValue;

    private Long contentId;
    private String contentType;
    private String title;
    private String body;
    private String dialect;
    private String style;
    private String mediaUrl;
    private Integer durationSec;
    /** AI增强 / 本地模板 */
    private String engine;
    /** 兜底提示：内容完备度不足时的降级说明 */
    private String degradeNote;
}
