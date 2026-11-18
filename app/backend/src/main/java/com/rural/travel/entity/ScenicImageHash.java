package com.rural.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("scenic_image_hash")
public class ScenicImageHash {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long scenicSpotId;
    private String imageUrl;
    private String hashValue;
    private LocalDateTime createdAt;
}
