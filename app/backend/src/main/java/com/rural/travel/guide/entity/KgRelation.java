package com.rural.travel.guide.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 知识图谱关系：BELONGS_TO / NEARBY / SAME_THEME / RECOMMEND_WITH / TRANSFER */
@Data
@TableName("kg_relation")
public class KgRelation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sourceId;
    private Long targetId;
    private String relationType;
    /** 关联强度 0~1 */
    private Double weight;
    /** 步行接驳时长 t_ij(分钟),式(2) */
    private Integer walkMinutes;
    private Integer status;
    private LocalDateTime createdAt;
}
