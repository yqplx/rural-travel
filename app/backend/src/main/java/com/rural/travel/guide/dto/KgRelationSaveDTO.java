package com.rural.travel.guide.dto;

import lombok.Data;

/** 管理端：知识图谱关系维护 */
@Data
public class KgRelationSaveDTO {
    private Long id;
    private Long sourceId;
    private Long targetId;
    /** BELONGS_TO / NEARBY / SAME_THEME / RECOMMEND_WITH / TRANSFER */
    private String relationType;
    private Double weight;
    /** 步行接驳时长 t_ij(分钟) */
    private Integer walkMinutes;
    private Integer status;
}
