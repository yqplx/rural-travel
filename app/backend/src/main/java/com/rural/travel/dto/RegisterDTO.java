package com.rural.travel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterDTO(
        @NotBlank(message = "请输入用户名")
        @Size(min = 3, max = 20, message = "用户名长度需为3-20位")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名仅支持字母、数字、下划线")
        String username,
        @NotBlank(message = "请输入密码")
        @Size(min = 6, max = 32, message = "密码长度需为6-32位")
        String password,
        @Size(max = 20, message = "昵称最长20位") String nickname,
        String province) {}
