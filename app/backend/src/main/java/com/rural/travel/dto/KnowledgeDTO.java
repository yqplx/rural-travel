package com.rural.travel.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class KnowledgeDTO {
    private Long id;
    @NotBlank(message = "请输入问题") private String question;
    @NotBlank(message = "请输入答案") private String answer;
    private String keywords;
    private String category;
    private Integer sortOrder;
    private Integer status;
}
