package com.rural.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("virtual_user")
public class VirtualUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String interestTags;
    private String description;
    private LocalDateTime createdAt;
}
