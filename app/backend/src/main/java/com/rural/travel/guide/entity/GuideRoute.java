package com.rural.travel.guide.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 个性化游览路线(S4) */
@Data
@TableName("guide_route")
public class GuideRoute {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String countyCode;
    private String name;
    /** T_u */
    private Integer planMinutes;
    /** Σw_i + Σt_ij,式(2)左式 */
    private Integer totalMinutes;
    /** Σs_u,i,式(1) */
    private Double totalScore;
    private Integer itemCount;
    private Integer avoidCrowd;
    private String status;
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private List<GuideRouteItem> items;
}
