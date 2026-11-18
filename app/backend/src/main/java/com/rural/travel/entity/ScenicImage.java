package com.rural.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("scenic_image")
public class ScenicImage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long scenicSpotId;
    private String imageUrl;
    private Integer sortOrder;
}
