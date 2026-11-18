package com.rural.travel.service;

import com.rural.travel.entity.User;
import com.rural.travel.mapper.UserMapper;
import com.rural.travel.utils.AesCryptoUtil;
import com.rural.travel.vo.AiConfigVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 用户自有大模型密钥管理
 * 每个用户使用自己的 Key,互不共享;未配置的用户自动降级本地引擎
 */
@Service
public class UserAiConfigService {

    private static final String DEFAULT_BASE_URL = "https://api.deepseek.com/v1";
    private static final String DEFAULT_MODEL = "deepseek-chat";

    private final UserMapper userMapper;
    private final AesCryptoUtil crypto;

    public UserAiConfigService(UserMapper userMapper, AesCryptoUtil crypto) {
        this.userMapper = userMapper;
        this.crypto = crypto;
    }

    /** 用户的大模型调用凭证,未配置返回 null */
    public record AiKey(String apiKey, String baseUrl, String model) {}

    /** 取当前用户可用凭证(已解密),未配置返回 null */
    public AiKey keyOf(Long userId) {
        if (userId == null) return null;
        User u = userMapper.selectById(userId);
        if (u == null || !StringUtils.hasText(u.getAiApiKeyEnc())) return null;
        String plain = crypto.decrypt(u.getAiApiKeyEnc());
        if (!StringUtils.hasText(plain)) return null;
        return new AiKey(plain,
                StringUtils.hasText(u.getAiBaseUrl()) ? u.getAiBaseUrl() : DEFAULT_BASE_URL,
                StringUtils.hasText(u.getAiChatModel()) ? u.getAiChatModel() : DEFAULT_MODEL);
    }

    /** 展示用的配置视图:只回显掩码,不含明文 */
    public AiConfigVO viewOf(Long userId) {
        User u = userMapper.selectById(userId);
        if (u == null) return new AiConfigVO(false, null, null, null, null);
        boolean configured = StringUtils.hasText(u.getAiApiKeyEnc());
        return new AiConfigVO(configured,
                configured ? u.getAiKeyMask() : null,
                StringUtils.hasText(u.getAiBaseUrl()) ? u.getAiBaseUrl() : DEFAULT_BASE_URL,
                StringUtils.hasText(u.getAiChatModel()) ? u.getAiChatModel() : DEFAULT_MODEL,
                u.getAiUpdatedAt());
    }

    /** 保存(更新)用户自己的密钥 */
    public void save(Long userId, String apiKey, String baseUrl, String model) {
        User u = userMapper.selectById(userId);
        if (u == null) return;
        if (StringUtils.hasText(apiKey)) {
            u.setAiApiKeyEnc(crypto.encrypt(apiKey.trim()));
            u.setAiKeyMask(AesCryptoUtil.mask(apiKey.trim()));
            u.setAiUpdatedAt(LocalDateTime.now());
        }
        u.setAiBaseUrl(StringUtils.hasText(baseUrl) ? baseUrl.trim() : DEFAULT_BASE_URL);
        u.setAiChatModel(StringUtils.hasText(model) ? model.trim() : DEFAULT_MODEL);
        userMapper.updateById(u);
    }

    /** 清除用户密钥 */
    public void clear(Long userId) {
        User u = userMapper.selectById(userId);
        if (u == null) return;
        u.setAiApiKeyEnc(null);
        u.setAiKeyMask(null);
        u.setAiUpdatedAt(null);
        userMapper.updateById(u);
    }
}
