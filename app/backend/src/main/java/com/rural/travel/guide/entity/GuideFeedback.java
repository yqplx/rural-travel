package com.rural.travel.guide.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 导览反馈(S6)：驱动式(4) 热度与满意度更新 */
@Data
@TableName("guide_feedback")
public class GuideFeedback {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long entityId;
    private Long contentId;
    /** 1~5 */
    private Integer score;
    private String comment;
    private LocalDateTime createdAt;
}
