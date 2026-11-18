package com.rural.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    @JsonIgnore
    private String password;
    private String nickname;
    private String realName;
    private String gender;
    private Integer age;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;
    private String occupation;
    private String region;
    private String email;
    private String phone;
    private String avatar;
    private String role;
    private Integer status;
    private String province;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ===== 用户自有大模型配置(全部 JsonIgnore,永不下发前端,明文只在服务端内存出现) =====
    @JsonIgnore
    private String aiApiKeyEnc;
    @JsonIgnore
    private String aiBaseUrl;
    @JsonIgnore
    private String aiChatModel;
    @JsonIgnore
    private String aiKeyMask;
    @JsonIgnore
    private LocalDateTime aiUpdatedAt;
}
