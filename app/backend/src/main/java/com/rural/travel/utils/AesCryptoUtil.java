package com.rural.travel.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 用户自有大模型密钥的加解密(AES-256-GCM)
 * 库中只存密文,明文仅在调用大模型时在内存中出现,不落日志、不下发前端
 */
@Component
public class AesCryptoUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec keySpec;
    private final SecureRandom random = new SecureRandom();

    public AesCryptoUtil(@Value("${rural.ai.crypto-secret:rural-travel-ai-crypto-2026}") String secret) {
        try {
            byte[] hashed = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            this.keySpec = new SecretKeySpec(hashed, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("AES 密钥初始化失败", e);
        }
    }

    /** 加密,返回 Base64(IV + 密文+认证标签) */
    public String encrypt(String plain) {
        if (plain == null) return null;
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_BITS, iv));
            byte[] body = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + body.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(body, 0, out, iv.length, body.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("密钥加密失败", e);
        }
    }

    /** 解密,失败返回 null(如密钥被更换或数据损坏) */
    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isBlank()) return null;
        try {
            byte[] all = Base64.getDecoder().decode(cipherText);
            if (all.length <= IV_LENGTH) return null;
            byte[] iv = new byte[IV_LENGTH];
            byte[] body = new byte[all.length - IV_LENGTH];
            System.arraycopy(all, 0, iv, 0, IV_LENGTH);
            System.arraycopy(all, IV_LENGTH, body, 0, body.length);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(body), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    /** 生成掩码:sk-370b6749c5bf4b1c91698f40a5f1e445 → sk-37****************************445 */
    public static String mask(String key) {
        if (key == null || key.isBlank()) return null;
        int n = key.length();
        if (n <= 8) return "****";
        int head = Math.min(5, n / 3);
        int tail = Math.min(3, n / 4);
        StringBuilder sb = new StringBuilder(key.substring(0, head));
        sb.append("*".repeat(Math.max(4, n - head - tail)));
        sb.append(key.substring(n - tail));
        return sb.toString();
    }
}
