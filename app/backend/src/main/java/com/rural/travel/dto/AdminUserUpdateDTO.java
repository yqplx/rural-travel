package com.rural.travel.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** 管理端 用户编辑 */
@Data
public class AdminUserUpdateDTO {
    private String nickname;
    private String email;
    private String phone;
    private String province;
    @Pattern(regexp = "^(USER|ADMIN)$", message = "角色仅支持 USER/ADMIN")
    private String role;
    private Integer status;
    private String newPassword;
}
