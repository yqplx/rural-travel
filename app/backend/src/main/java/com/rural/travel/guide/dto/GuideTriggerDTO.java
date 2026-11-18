package com.rural.travel.guide.dto;

import lombok.Data;

/** S5 位置触发讲解上报 */
@Data
public class GuideTriggerDTO {
    /** 游客当前经纬度（S5 位置触发） */
    private Double lng;
    private Double lat;
    /** 已指定实体则不再做最近邻判定（例如点击卡片手动播放） */
    private Long entityId;
    /** 所属路线，可空 */
    private Long routeId;
    /** 在该实体已停留秒数，式(3) 的 r_u */
    private Integer dwellSeconds = 0;
    /** 该会话已提问次数，式(3) 的 q_u */
    private Integer askCount = 0;
    /** 语音方言：普通话 / 方言 */
    private String dialect;
    /** 路线内序号，用于推断已游览节点 */
    private Integer seq;
}
