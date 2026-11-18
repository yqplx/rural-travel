package com.rural.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("virtual_rating")
public class VirtualRating {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long virtualUserId;
    private Long scenicSpotId;
    private Integer score;
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String virtualUserName;
    @TableField(exist = false)
    private String scenicName;
}
