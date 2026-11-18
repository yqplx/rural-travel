package com.rural.travel.controller;

import com.rural.travel.common.PageResult;
import com.rural.travel.common.Result;
import com.rural.travel.dto.AdminUserUpdateDTO;
import com.rural.travel.entity.User;
import com.rural.travel.security.SecurityUtils;
import com.rural.travel.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** 管理端-用户管理 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public Result<PageResult<User>> page(@RequestParam(defaultValue = "1") long current,
                                         @RequestParam(defaultValue = "10") long size,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) String role) {
        return Result.ok(userService.adminPage(current, size, keyword, role));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody AdminUserUpdateDTO dto) {
        userService.adminUpdate(id, dto, SecurityUtils.current().getUsername());
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.adminDelete(id, SecurityUtils.currentUserId(), SecurityUtils.current().getUsername());
        return Result.ok();
    }
}
