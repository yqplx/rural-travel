package com.rural.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("browse_history")
public class BrowseHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long scenicSpotId;
    private Integer duration;
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private ScenicSpot scenicSpot;
}
