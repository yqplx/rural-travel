package com.rural.travel.dto;

import lombok.Data;

import java.math.BigDecimal;

/** 景区多条件筛选 */
@Data
public class ScenicQueryDTO {
    private String keyword;
    private Long categoryId;
    private Long tagId;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    /** score|price_asc|price_desc|view|newest */
    private String sort;
    private Long current = 1L;
    private Long size = 12L;
}
