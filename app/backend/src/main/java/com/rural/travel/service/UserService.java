package com.rural.travel.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rural.travel.common.PageResult;
import com.rural.travel.dto.AdminUserUpdateDTO;
import com.rural.travel.dto.LoginDTO;
import com.rural.travel.dto.ProfileUpdateDTO;
import com.rural.travel.dto.RegisterDTO;
import com.rural.travel.entity.User;
import com.rural.travel.exception.BizException;
import com.rural.travel.mapper.UserMapper;
import com.rural.travel.security.JwtUtils;
import com.rural.travel.vo.LoginVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final SysLogService sysLogService;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder,
                       JwtUtils jwtUtils, SysLogService sysLogService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.sysLogService = sysLogService;
    }

    public LoginVO register(RegisterDTO dto) {
        Long exists = userMapper.selectCount(new QueryWrapper<User>().eq("username", dto.username()));
        if (exists > 0) {
            throw new BizException("用户名已被注册，换一个试试");
        }
        User user = new User();
        user.setUsername(dto.username());
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setNickname(StringUtils.hasText(dto.nickname()) ? dto.nickname() : dto.username());
        user.setProvince(dto.province());
        user.setRole("USER");
        user.setStatus(1);
        userMapper.insert(user);
        sysLogService.save(user.getId(), user.getUsername(), "注册", "新用户注册");
        return new LoginVO(jwtUtils.generate(user.getId(), user.getUsername(), user.getRole()), user);
    }

    public LoginVO login(LoginDTO dto) {
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", dto.username()));
        if (user == null || !passwordEncoder.matches(dto.password(), user.getPassword())) {
            throw new BizException("用户名或密码不正确");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException(403, "账号已被禁用，请联系管理员");
        }
        sysLogService.save(user.getId(), user.getUsername(), "登录", "登录成功");
        return new LoginVO(jwtUtils.generate(user.getId(), user.getUsername(), user.getRole()), user);
    }

    public User getById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BizException(404, "用户不存在");
        return user;
    }

    public User updateProfile(Long userId, ProfileUpdateDTO dto) {
        User user = getById(userId);
        if (StringUtils.hasText(dto.nickname())) user.setNickname(dto.nickname());
        if (dto.realName() != null) user.setRealName(dto.realName());
        if (dto.gender() != null) user.setGender(dto.gender());
        if (dto.age() != null) user.setAge(dto.age());
        if (dto.birthday() != null && !dto.birthday().isBlank()) {
            user.setBirthday(LocalDate.parse(dto.birthday()));
        }
        if (dto.occupation() != null) user.setOccupation(dto.occupation());
        if (dto.region() != null) user.setRegion(dto.region());
        if (dto.email() != null) user.setEmail(dto.email());
        if (dto.phone() != null) user.setPhone(dto.phone());
        if (dto.avatar() != null) user.setAvatar(dto.avatar());
        if (dto.province() != null) user.setProvince(dto.province());
        userMapper.updateById(user);
        return user;
    }

    private static final java.util.regex.Pattern PASSWORD_RULE =
            java.util.regex.Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,20}$");

    /** 修改密码:校验原密码 + 新密码强度(8-20位,含字母和数字) */
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        if (!PASSWORD_RULE.matcher(newPassword).matches()) {
            throw new BizException("新密码需为8-20位,且同时包含字母和数字");
        }
        User user = getById(userId);
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BizException("原密码不正确");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        sysLogService.save(userId, user.getUsername(), "修改密码", "用户自助修改密码");
    }

    /** 注销账号:验证密码后停用(数据保留,无法登录) */
    public void deactivate(Long userId, String password) {
        User user = getById(userId);
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BizException("密码不正确,注销失败");
        }
        user.setStatus(0);
        userMapper.updateById(user);
        sysLogService.save(userId, user.getUsername(), "注销账号", "用户自助注销,账号已停用");
    }

    // ==================== 管理端 ====================

    public PageResult<User> adminPage(long current, long size, String keyword, String role) {
        Page<User> page = userMapper.selectPage(new Page<>(current, size),
                new QueryWrapper<User>()
                        .and(StringUtils.hasText(keyword), w -> w.like("username", keyword).or().like("nickname", keyword))
                        .eq(StringUtils.hasText(role), "role", role)
                        .orderByDesc("id"));
        return PageResult.of(page);
    }

    public void adminUpdate(Long id, AdminUserUpdateDTO dto, String operator) {
        User user = getById(id);
        if (StringUtils.hasText(dto.getNickname())) user.setNickname(dto.getNickname());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        if (dto.getProvince() != null) user.setProvince(dto.getProvince());
        if (StringUtils.hasText(dto.getRole())) user.setRole(dto.getRole());
        if (dto.getStatus() != null) user.setStatus(dto.getStatus());
        if (StringUtils.hasText(dto.getNewPassword())) {
            if (dto.getNewPassword().length() < 6) throw new BizException("新密码至少6位");
            user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        }
        userMapper.updateById(user);
        sysLogService.save(null, operator, "编辑用户", "修改用户 " + user.getUsername());
    }

    public void adminDelete(Long id, Long operatorId, String operator) {
        if (id.equals(operatorId)) throw new BizException("不能删除当前登录的管理员账号");
        User user = getById(id);
        userMapper.deleteById(id);
        sysLogService.save(operatorId, operator, "删除用户", "删除用户 " + user.getUsername());
    }
}
