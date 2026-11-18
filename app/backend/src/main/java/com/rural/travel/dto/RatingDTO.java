package com.rural.travel.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RatingDTO(
        @NotNull(message = "缺少景区id") Long scenicSpotId,
        @NotNull(message = "请选择评分")
        @Min(value = 1, message = "评分范围为1-5星")
        @Max(value = 5, message = "评分范围为1-5星") Integer score,
        @Size(max = 500, message = "评价最长500字") String comment) {}
