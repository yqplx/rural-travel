package com.rural.travel.guide.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 文旅知识图谱实体：景点/非遗/美食/村落/传承人/技艺 */
@Data
@TableName("kg_entity")
public class KgEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String countyCode;
    /** SCENIC景点 / HERITAGE非遗 / FOOD美食 / VILLAGE村落 / PERSON传承人 / CRAFT技艺 */
    private String entityType;
    /** entityType=SCENIC 时关联 scenic_spot.id */
    private Long refId;
    private String name;
    private String summary;
    private String detail;
    private String coverImage;
    private Double lng;
    private Double lat;
    /** 逗号分隔标签 */
    private String tags;
    /** 建议游览时长 w_i(分钟) */
    private Integer visitMinutes;
    /** 拥挤度 c_i,0~1,式(1)取负号体现"避堵" */
    private Double crowd;
    /** 热度 h_i,0~1,式(4)指数平滑更新 */
    private Double heat;
    /** 内容完备度 g_i,0~1,由 mm_content 覆盖度算得 */
    private Double contentScore;
    private String openTime;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ---- 非持久化扩展字段 ----
    @TableField(exist = false)
    private List<String> tagList;
    /** 该实体已挂载的内容数 */
    @TableField(exist = false)
    private Integer contentCount;
    /** 图谱可视化：关系数量(度) */
    @TableField(exist = false)
    private Integer degree;
}
