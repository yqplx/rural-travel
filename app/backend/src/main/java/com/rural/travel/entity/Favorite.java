package com.rural.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("favorite")
public class Favorite {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long scenicSpotId;
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private ScenicSpot scenicSpot;
}
