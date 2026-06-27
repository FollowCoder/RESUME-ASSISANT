package com.resume.agent.writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对话上下文存储 - 使用内存 ConcurrentHashMap 管理会话
 */
@Component
public class ConversationStore {

    private static final Logger log = LoggerFactory.getLogger(ConversationStore.class);

    /**
     * 会话超时时间：30 分钟
     */
    private static final long SESSION_TIMEOUT_MS = 30 * 60 * 1000L;

    private final Map<String, ConversationContext> sessions = new ConcurrentHashMap<>();

    /**
     * 获取或创建会话上下文
     */
    public ConversationContext getOrCreate(String sessionId, Long userId) {
        return sessions.computeIfAbsent(sessionId, id -> {
            log.info("创建新会话: sessionId={}, userId={}", sessionId, userId);
            return new ConversationContext(sessionId, userId);
        });
    }

    /**
     * 获取已有会话
     */
    public ConversationContext get(String sessionId) {
        ConversationContext ctx = sessions.get(sessionId);
        if (ctx != null) {
            ctx.setLastAccessTime(System.currentTimeMillis());
        }
        return ctx;
    }

    /**
     * 移除会话
     */
    public void remove(String sessionId) {
        sessions.remove(sessionId);
        log.info("移除会话: sessionId={}", sessionId);
    }

    /**
     * 定时清理过期会话（每5分钟执行一次）
     */
    @Scheduled(fixedRate = 300000)
    public void cleanExpiredSessions() {
        long now = System.currentTimeMillis();
        sessions.entrySet().removeIf(entry -> {
            boolean expired = (now - entry.getValue().getLastAccessTime()) > SESSION_TIMEOUT_MS;
            if (expired) {
                log.info("清理过期会话: sessionId={}", entry.getKey());
            }
            return expired;
        });
    }
}
