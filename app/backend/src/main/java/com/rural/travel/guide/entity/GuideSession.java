package com.rural.travel.guide.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 位置触发自适应讲解会话(S5) */
@Data
@TableName("guide_session")
public class GuideSession {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 游客可匿名导览，故可空 */
    private Long userId;
    private Long routeId;
    private Long entityId;
    private Long contentId;
    private Double triggerLng;
    private Double triggerLat;
    private Integer distanceM;
    /** d_u,式(3) */
    private Integer depthLevel;
    /** 判定明细，便于界面解释"为什么给你这一档" */
    private String depthDetail;
    /** 停留秒数，式(3) 的 r_u */
    private Integer dwellSeconds;
    /** 已提问次数，式(3) 的 q_u */
    private Integer askCount;
    private LocalDateTime createdAt;
}
