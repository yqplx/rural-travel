package com.rural.travel.security;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 登录上下文(存入 SecurityContext) */
@Data
@AllArgsConstructor
public class LoginUser {
    private Long id;
    private String username;
    private String role;
}
