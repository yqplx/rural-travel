package com.rural.travel.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VirtualRatingSaveDTO {
    private Long id;
    @NotNull(message = "请选择虚拟用户") private Long virtualUserId;
    @NotNull(message = "请选择景区") private Long scenicSpotId;
    @NotNull(message = "请选择评分")
    @Min(value = 1, message = "评分范围为1-5星")
    @Max(value = 5, message = "评分范围为1-5星") private Integer score;
}
