package com.rural.travel.vo;

import com.rural.travel.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginVO {
    private String token;
    private User user;
}
