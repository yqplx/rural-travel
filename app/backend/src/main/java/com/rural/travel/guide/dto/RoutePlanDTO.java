package com.rural.travel.guide.dto;

import lombok.Data;

import java.util.List;

/** S4 路线规划请求 */
@Data
public class RoutePlanDTO {
    /** 县域编码，留空取默认县域 */
    private String countyCode;
    /** 计划游览时长 T_u(分钟)，留空取画像值，默认 240 */
    private Integer planMinutes;
    /** 是否避堵（硬过滤拥挤度过高节点） */
    private Integer avoidCrowd = 1;
    /** 限定实体类型，留空不限 */
    private List<String> entityTypes;
    /** 最多节点数，默认 8 */
    private Integer maxItems = 8;
    /** 是否已绑定路线的起始实体（例如从当前所在景点继续） */
    private Long startEntityId;
}
