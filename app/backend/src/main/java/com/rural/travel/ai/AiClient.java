package com.rural.travel.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容大模型客户端
 * 未配置 API Key 或调用失败时返回 null,由各服务降级到本地轻量引擎
 */
@Slf4j
@Component
public class AiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${rural.ai.api-key:}")
    private String apiKey;
    @Value("${rural.ai.base-url:https://api.deepseek.com/v1}")
    private String baseUrl;
    @Value("${rural.ai.chat-model:deepseek-chat}")
    private String chatModel;
    @Value("${rural.ai.vision-model:}")
    private String visionModel;

    public AiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean chatEnabled() {
        return StringUtils.hasText(apiKey);
    }

    public boolean visionEnabled() {
        return chatEnabled() && StringUtils.hasText(visionModel);
    }

    /** 文本对话,失败返回 null */
    public String chat(String systemPrompt, String userMessage) {
        if (!chatEnabled()) return null;
        try {
            Map<String, Object> body = Map.of(
                    "model", chatModel,
                    "temperature", 0.7,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userMessage)));
            String resp = post(baseUrl + "/chat/completions", body);
            JsonNode node = objectMapper.readTree(resp);
            return node.path("choices").get(0).path("message").path("content").asText(null);
        } catch (Exception e) {
            log.warn("大模型对话调用失败,降级本地引擎: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 使用「用户自己的密钥」对话,失败返回 null 由调用方降级本地引擎。
     * 这是推荐的调用方式:谁的 Key 谁消耗额度。
     */
    public String chatWith(String apiKey, String baseUrl, String model,
                           String systemPrompt, String userMessage) {
        if (!StringUtils.hasText(apiKey)) return null;
        String url = StringUtils.hasText(baseUrl) ? baseUrl : this.baseUrl;
        String m = StringUtils.hasText(model) ? model : this.chatModel;
        try {
            Map<String, Object> body = Map.of(
                    "model", m,
                    "temperature", 0.7,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userMessage)));
            String resp = post(url + "/chat/completions", body, apiKey);
            JsonNode node = objectMapper.readTree(resp);
            return node.path("choices").get(0).path("message").path("content").asText(null);
        } catch (Exception e) {
            log.warn("用户密钥调用大模型失败,降级本地引擎: {}", e.getMessage());
            return null;
        }
    }

    /** 用指定密钥做一次连通性测试,返回是否可用 */
    public boolean testKey(String apiKey, String baseUrl, String model) {
        if (!StringUtils.hasText(apiKey)) return false;
        try {
            String r = chatWith(apiKey, baseUrl, model, "你只回复:ok", "ping");
            return r != null && !r.isBlank();
        } catch (Exception e) {
            return false;
        }
    }

    /** 多模态识图(全局密钥,通常不启用),失败返回 null */
    public String vision(String prompt, String base64Image) {
        if (!visionEnabled()) return null;
        try {
            Map<String, Object> body = Map.of(
                    "model", visionModel,
                    "messages", List.of(Map.of("role", "user", "content", List.of(
                            Map.of("type", "text", "text", prompt),
                            Map.of("type", "image_url", "image_url",
                                    Map.of("url", "data:image/jpeg;base64," + base64Image))))));
            String resp = post(baseUrl + "/chat/completions", body);
            JsonNode node = objectMapper.readTree(resp);
            return node.path("choices").get(0).path("message").path("content").asText(null);
        } catch (Exception e) {
            log.warn("多模态识别调用失败,降级本地引擎: {}", e.getMessage());
            return null;
        }
    }

    private String post(String url, Map<String, Object> body, String key) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(key);
        return restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class).getBody();
    }

    private String post(String url, Map<String, Object> body) {
        return post(url, body, apiKey);
    }
}
