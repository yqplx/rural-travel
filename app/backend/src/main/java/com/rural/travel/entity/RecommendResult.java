package com.rural.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("recommend_result")
public class RecommendResult {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long scenicSpotId;
    private Double score;
    private String reason;
    private LocalDateTime createdAt;
}
