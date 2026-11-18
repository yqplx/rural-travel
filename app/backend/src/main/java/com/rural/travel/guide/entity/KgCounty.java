package com.rural.travel.guide.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 县域主体：支撑"一套系统多县域复制" */
@Data
@TableName("kg_county")
public class KgCounty {
    @TableId(type = IdType.INPUT)
    private String code;
    private String name;
    private String province;
    private String coverImage;
    private String description;
    private Integer status;
    private LocalDateTime createdAt;
}
