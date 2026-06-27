package com.resume.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    private SecurityUtils() {
        // 工具类禁止实例化
    }

    /**
     * 从 SecurityContext 获取当前登录用户的 ID。
     * JwtAuthenticationFilter 将 userId (Long) 设置为 Authentication 的 principal。
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException("用户未登录");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long userId) {
            return userId;
        }
        throw new BusinessException("无法获取用户信息");
    }
}
