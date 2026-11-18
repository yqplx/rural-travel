package com.rural.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("knowledge_entry")
public class KnowledgeEntry {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String question;
    private String answer;
    private String keywords;
    private String category;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
