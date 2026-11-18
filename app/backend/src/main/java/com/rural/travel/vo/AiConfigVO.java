package com.rural.travel.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/** 用户 AI 配置视图:仅包含掩码与厂商信息,绝不包含明文密钥 */
public record AiConfigVO(
        boolean configured,
        String mask,
        String baseUrl,
        String model,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime updatedAt
) {}
