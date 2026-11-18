package com.rural.travel.guide.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 路线节点(S4)：式(1)得分 + 式(2)时长与接驳 */
@Data
@TableName("guide_route_item")
public class GuideRouteItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long routeId;
    private Long entityId;
    private Integer seq;
    /** 自出发起算的到达分钟 */
    private Integer arriveMinute;
    /** w_i 停留分钟 */
    private Integer stayMinutes;
    /** t_ij 接驳分钟 */
    private Integer transferMin;
    /** s_u,i,式(1) */
    private Double score;
    /** 入选理由，保证可解释 */
    private String reason;

    @TableField(exist = false)
    private String entityName;
    @TableField(exist = false)
    private String entityType;
    @TableField(exist = false)
    private String coverImage;
    @TableField(exist = false)
    private Double lng;
    @TableField(exist = false)
    private Double lat;
}
