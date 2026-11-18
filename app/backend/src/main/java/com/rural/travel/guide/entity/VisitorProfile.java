package com.rural.travel.guide.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 游客画像：驱动"千人千面"路线与讲解 */
@Data
@TableName("visitor_profile")
public class VisitorProfile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    /** 青年 / 中年 / 银发 / 亲子 */
    private String ageBand;
    /** solo / couple / family / friends / team */
    private String groupType;
    /** 逗号分隔兴趣标签 */
    private String interests;
    /** slow / normal / fast */
    private String pace;
    private String preferDialect;
    /** 式(3) 的 k_u 知识背景水平，1~3，取值越高讲解越深 */
    private Integer depthLevel;
    /** 计划游览时长 T_u(分钟),式(2) */
    private Integer planMinutes;
    private Integer avoidCrowd;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private List<String> interestList;
}
