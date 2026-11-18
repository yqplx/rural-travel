package com.rural.travel.controller;

import com.rural.travel.common.Result;
import com.rural.travel.dto.AiConfigDTO;
import com.rural.travel.dto.ProfileUpdateDTO;
import com.rural.travel.entity.User;
import com.rural.travel.recommend.RecommendService;
import com.rural.travel.security.SecurityUtils;
import com.rural.travel.service.InterestService;
import com.rural.travel.service.UserAiConfigService;
import com.rural.travel.service.UserService;
import com.rural.travel.vo.AiConfigVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 个人中心 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final InterestService interestService;
    private final RecommendService recommendService;
    private final UserAiConfigService userAiConfigService;
    private final com.rural.travel.ai.AiClient aiClient;

    public UserController(UserService userService, InterestService interestService,
                          RecommendService recommendService, UserAiConfigService userAiConfigService,
                          com.rural.travel.ai.AiClient aiClient) {
        this.userService = userService;
        this.interestService = interestService;
        this.recommendService = recommendService;
        this.userAiConfigService = userAiConfigService;
        this.aiClient = aiClient;
    }

    @GetMapping("/profile")
    public Result<User> profile() {
        return Result.ok(userService.getById(SecurityUtils.currentUserId()));
    }

    @PutMapping("/profile")
    public Result<User> updateProfile(@Valid @RequestBody ProfileUpdateDTO dto) {
        return Result.ok(userService.updateProfile(SecurityUtils.currentUserId(), dto));
    }

    public record InterestDTO(@NotEmpty(message = "请至少勾选一个感兴趣的偏好") List<Long> tagIds) {}

    public record ChangePasswordDTO(
            @jakarta.validation.constraints.NotBlank(message = "请输入原密码") String oldPassword,
            @jakarta.validation.constraints.NotBlank(message = "请输入新密码") String newPassword) {}

    public record PasswordDTO(
            @jakarta.validation.constraints.NotBlank(message = "请输入密码以确认操作") String password) {}

    /** 当前用户已勾选的偏好标签 */
    @GetMapping("/interests")
    public Result<List<Long>> interests() {
        return Result.ok(interestService.tagIdsOf(SecurityUtils.currentUserId()));
    }

    /** 保存偏好勾选,并立即刷新首页个性化推荐 */
    @PutMapping("/interests")
    public Result<Void> setInterests(@Valid @RequestBody InterestDTO dto) {
        Long userId = SecurityUtils.currentUserId();
        interestService.setInterests(userId, dto.tagIds());
        recommendService.recompute(userId);
        return Result.ok();
    }

    /** 修改密码 */
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        userService.changePassword(SecurityUtils.currentUserId(), dto.oldPassword(), dto.newPassword());
        return Result.ok();
    }

    /** 注销账号(验证密码后停用,数据保留) */
    @PostMapping("/deactivate")
    public Result<Void> deactivate(@Valid @RequestBody PasswordDTO dto) {
        userService.deactivate(SecurityUtils.currentUserId(), dto.password());
        return Result.ok();
    }

    // ===== 我的大模型密钥:每人用自己的,互不共享 =====

    /** 查看自己的 AI 配置(只返回掩码,不返回明文) */
    @GetMapping("/ai-config")
    public Result<AiConfigVO> aiConfig() {
        return Result.ok(userAiConfigService.viewOf(SecurityUtils.currentUserId()));
    }

    /** 保存自己的密钥;apiKey 留空表示仅修改厂商地址/模型名 */
    @PutMapping("/ai-config")
    public Result<AiConfigVO> saveAiConfig(@Valid @RequestBody AiConfigDTO dto) {
        Long userId = SecurityUtils.currentUserId();
        userAiConfigService.save(userId, dto.apiKey(), dto.baseUrl(), dto.model());
        return Result.ok(userAiConfigService.viewOf(userId));
    }

    /** 清除自己的密钥,回到本地引擎模式 */
    @DeleteMapping("/ai-config")
    public Result<Void> clearAiConfig() {
        userAiConfigService.clear(SecurityUtils.currentUserId());
        return Result.ok();
    }

    /** 连通性测试:用提交的密钥发一次最小请求,不落库 */
    @PostMapping("/ai-config/test")
    public Result<Boolean> testAiConfig(@Valid @RequestBody AiConfigDTO dto) {
        String key = dto.apiKey();
        if (!org.springframework.util.StringUtils.hasText(key)) {
            var cur = userAiConfigService.keyOf(SecurityUtils.currentUserId());
            if (cur == null) return Result.ok(false);
            return Result.ok(aiClient.testKey(cur.apiKey(), cur.baseUrl(), cur.model()));
        }
        return Result.ok(aiClient.testKey(key.trim(), dto.baseUrl(), dto.model()));
    }
}
