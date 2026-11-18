package com.rural.travel.security;

import com.rural.travel.exception.BizException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** 获取当前登录用户 */
public class SecurityUtils {

    public static LoginUser current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser u) {
            return u;
        }
        throw new BizException(401, "登录已过期或尚未登录，请重新登录");
    }

    public static Long currentUserId() {
        return current().getId();
    }

    /** 游客访问时返回 null,不抛异常 */
    public static Long currentUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser u) {
            return u.getId();
        }
        return null;
    }
}
