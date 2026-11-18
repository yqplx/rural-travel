package com.rural.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

@Data
@TableName("scenic_spot")
public class ScenicSpot {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Long categoryId;
    private String tagIds;
    private String province;
    private String city;
    private String address;
    private String description;
    private String features;
    private String tips;
    private BigDecimal ticketPrice;
    private String openTime;
    private String coverImage;
    private BigDecimal avgScore;
    private Integer scoreCount;
    private Integer viewCount;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ---- 非持久化扩展字段 ----
    @TableField(exist = false)
    private String categoryName;
    @TableField(exist = false)
    private List<String> tagNames;
    @TableField(exist = false)
    private List<ScenicImage> images;
    @TableField(exist = false)
    private String reason;
    @TableField(exist = false)
    private Boolean favorited;
    @TableField(exist = false)
    private Integer myScore;
}
