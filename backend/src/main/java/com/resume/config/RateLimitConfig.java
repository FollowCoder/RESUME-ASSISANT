package com.resume.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 简单的 API 限流配置 - 按 IP/用户每分钟最多 30 次请求（排除 WebSocket）
 */
@Configuration
public class RateLimitConfig {

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration() {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RateLimitFilter());
        registration.addUrlPatterns("/api/*");
        registration.setOrder(1);
        return registration;
    }

    /**
     * 基于滑动窗口的简单限流 Filter
     */
    public static class RateLimitFilter extends OncePerRequestFilter {

        private static final int MAX_REQUESTS_PER_MINUTE = 30;
        private static final long WINDOW_MS = 60_000L;
        private static final long CACHE_EXPIRE_MINUTES = 10; // 缓存过期时间

        // 使用 Caffeine 缓存，自动过期清理
        private final Cache<String, RequestCounter> counters = Caffeine.newBuilder()
                .expireAfterAccess(CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES)
                .maximumSize(1000) // 最大缓存条目数
                .build();

        @Override
        protected void doFilterInternal(@NonNull HttpServletRequest request,
                                        @NonNull HttpServletResponse response,
                                        @NonNull FilterChain filterChain)
                throws ServletException, IOException {

            // 排除 WebSocket 升级请求
            String upgrade = request.getHeader("Upgrade");
            if ("websocket".equalsIgnoreCase(upgrade)) {
                filterChain.doFilter(request, response);
                return;
            }

            String key = resolveKey(request);
            RequestCounter counter = counters.get(key, k -> new RequestCounter());

            if (!counter.tryAcquire()) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"success\":false,\"message\":\"请求过于频繁，请稍后再试\"}");
                return;
            }

            filterChain.doFilter(request, response);
        }

        /**
         * 解析限流 Key：优先用认证用户ID，否则用 IP 地址
         */
        private String resolveKey(HttpServletRequest request) {
            // 尝试从 Authorization header 中提取用户身份
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                // 使用 token 前缀作为 key（避免解析开销）
                String token = authHeader.substring(7);
                return "user:" + token.substring(0, Math.min(token.length(), 20));
            }
            // 否则按 IP 限流
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isBlank()) {
                ip = request.getRemoteAddr();
            } else {
                ip = ip.split(",")[0].trim();
            }
            return "ip:" + ip;
        }

        /**
         * 简单的滑动窗口计数器
         */
        private static class RequestCounter {
            private final AtomicInteger count = new AtomicInteger(0);
            private volatile long windowStart = System.currentTimeMillis();

            public synchronized boolean tryAcquire() {
                long now = System.currentTimeMillis();
                if (now - windowStart > WINDOW_MS) {
                    // 重置窗口
                    count.set(0);
                    windowStart = now;
                }
                return count.incrementAndGet() <= MAX_REQUESTS_PER_MINUTE;
            }
        }
    }
}
