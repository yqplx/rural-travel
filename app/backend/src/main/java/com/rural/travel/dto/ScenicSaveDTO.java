package com.rural.travel.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** 管理端 新增/编辑景区 */
@Data
public class ScenicSaveDTO {
    private Long id;
    @NotBlank(message = "请输入景区名称")
    @Size(max = 80, message = "名称最长80字")
    private String name;
    @NotNull(message = "请选择分类") private Long categoryId;
    private List<Long> tagIds;
    private String province;
    private String city;
    private String address;
    private String description;
    private String features;
    private String tips;
    @NotNull(message = "请填写门票价格")
    @DecimalMin(value = "0", message = "价格不能为负") private BigDecimal ticketPrice;
    private String openTime;
    private String coverImage;
    private List<String> images;
    private Integer status;
}
