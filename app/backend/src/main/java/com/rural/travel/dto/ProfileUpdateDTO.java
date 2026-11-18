package com.rural.travel.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileUpdateDTO(
        @NotBlank(message = "请输入昵称")
        @Size(max = 20, message = "昵称最长20位") String nickname,
        @Size(max = 30, message = "姓名最长30位") String realName,
        String gender,
        @Min(value = 1, message = "年龄需在1-120之间")
        @Max(value = 120, message = "年龄需在1-120之间") Integer age,
        String birthday,
        @Size(max = 30, message = "职业最长30位") String occupation,
        @Size(max = 60, message = "所在地区最长60位") String region,
        String email,
        String phone,
        String avatar,
        String province) {}
