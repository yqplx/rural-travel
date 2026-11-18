package com.rural.travel.dto;

import jakarta.validation.constraints.Size;

/** 用户配置自己的大模型密钥;apiKey 为空表示仅修改厂商/模型,不更换密钥 */
public record AiConfigDTO(
        @Size(max = 200, message = "密钥过长") String apiKey,
        @Size(max = 255, message = "接口地址过长") String baseUrl,
        @Size(max = 64, message = "模型名过长") String model
) {}
