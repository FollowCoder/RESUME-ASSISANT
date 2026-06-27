package com.resume.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.agent.interviewer.InterviewContext;
import com.resume.agent.interviewer.InterviewContextStore;
import com.resume.model.dto.InterviewMessage;
import com.resume.model.entity.Interview;
import com.resume.repository.InterviewRepository;
import com.resume.service.InterviewAgentService;
import com.resume.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 面试 WebSocket 处理器
 */
@Component
public class InterviewWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(InterviewWebSocketHandler.class);
    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final InterviewAgentService interviewAgentService;
    private final InterviewContextStore contextStore;
    private final InterviewRepository interviewRepository;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    // 维护 session -> interviewId 的映射
    private final Map<String, Long> sessionInterviewMap = new ConcurrentHashMap<>();

    public InterviewWebSocketHandler(InterviewAgentService interviewAgentService,
                                     InterviewContextStore contextStore,
                                     InterviewRepository interviewRepository,
                                     JwtUtil jwtUtil,
                                     ObjectMapper objectMapper) {
        this.interviewAgentService = interviewAgentService;
        this.contextStore = contextStore;
        this.interviewRepository = interviewRepository;
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        try {
            // 从 URL 提取 interview ID 和 token
            String uri = session.getUri().toString();
            Long interviewId = extractInterviewId(uri);
            String token = extractToken(uri);

            if (interviewId == null) {
                sendError(session, "无效的面试ID");
                session.close(CloseStatus.BAD_DATA);
                return;
            }

            // 验证 token
            Long userId = null;
            if (token != null && jwtUtil.isTokenValid(token)) {
                userId = jwtUtil.extractUserId(token);
            }
            if (userId == null) {
                sendError(session, "身份验证失败，请重新登录");
                session.close(CloseStatus.POLICY_VIOLATION);
                return;
            }

            // 验证面试记录存在且属于当前用户
            Optional<Interview> interviewOpt = interviewRepository.findById(interviewId);
            if (interviewOpt.isEmpty()) {
                sendError(session, "面试记录不存在");
                session.close(CloseStatus.BAD_DATA);
                return;
            }

            Interview interview = interviewOpt.get();
            if (!interview.getUserId().equals(userId)) {
                sendError(session, "无权访问该面试");
                session.close(CloseStatus.POLICY_VIOLATION);
                return;
            }

            // 记录映射
            sessionInterviewMap.put(session.getId(), interviewId);

            // 获取面试上下文
            InterviewContext context = contextStore.get(interviewId);
            if (context == null) {
                sendError(session, "面试上下文不存在，请先通过API启动面试");
                session.close(CloseStatus.BAD_DATA);
                return;
            }

            // 发送系统消息
            sendMessage(session, InterviewMessage.builder()
                    .type("system")
                    .content("面试连接已建立，面试即将开始...")
                    .timestamp(now())
                    .metadata(Map.of("stage", context.getCurrentStage().name()))
                    .build());

            // 生成面试官开场白
            String opening = interviewAgentService.generateOpening(String.valueOf(interviewId), context);

            InterviewMessage openingMsg = InterviewMessage.builder()
                    .type("interviewer")
                    .content(opening)
                    .timestamp(now())
                    .metadata(Map.of("stage", context.getCurrentStage().name()))
                    .build();

            context.addMessage(openingMsg);
            sendMessage(session, openingMsg);

            log.info("面试 WebSocket 连接建立: interviewId={}, userId={}", interviewId, userId);

        } catch (Exception e) {
            log.error("WebSocket 连接建立失败", e);
            sendError(session, "连接建立失败: " + e.getMessage());
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long interviewId = sessionInterviewMap.get(session.getId());
        if (interviewId == null) {
            sendError(session, "会话未建立面试关联");
            return;
        }

        InterviewContext context = contextStore.get(interviewId);
        if (context == null) {
            sendError(session, "面试上下文已失效");
            return;
        }

        try {
            // 解析用户消息
            JsonNode msgNode = objectMapper.readTree(message.getPayload());
            String content = msgNode.path("content").asText("");

            if (content.isBlank()) {
                sendError(session, "消息内容不能为空");
                return;
            }

            // 记录候选人消息
            InterviewMessage candidateMsg = InterviewMessage.builder()
                    .type("candidate")
                    .content(content)
                    .timestamp(now())
                    .build();
            context.addMessage(candidateMsg);

            // 检查是否为面试结束状态
            if (context.getCurrentStage() == InterviewContext.Stage.COMPLETED) {
                sendMessage(session, InterviewMessage.builder()
                        .type("system")
                        .content("面试已结束，请通过API获取面试报告。")
                        .timestamp(now())
                        .metadata(Map.of("stage", "COMPLETED", "finished", true))
                        .build());
                return;
            }

            // 调用面试官处理回答
            String reply = interviewAgentService.processAnswer(String.valueOf(interviewId), content, context);

            InterviewMessage interviewerMsg = InterviewMessage.builder()
                    .type("interviewer")
                    .content(reply)
                    .timestamp(now())
                    .metadata(Map.of("stage", context.getCurrentStage().name()))
                    .build();
            context.addMessage(interviewerMsg);

            sendMessage(session, interviewerMsg);

            // 如果面试官发出结束消息，通知前端
            if (context.getCurrentStage() == InterviewContext.Stage.COMPLETED) {
                sendMessage(session, InterviewMessage.builder()
                        .type("system")
                        .content("面试已结束，正在生成评估报告...")
                        .timestamp(now())
                        .metadata(Map.of("stage", "COMPLETED", "finished", true))
                        .build());

                // 持久化对话记录
                persistConversation(interviewId, context);
            }

        } catch (Exception e) {
            log.error("处理面试消息失败: interviewId={}", interviewId, e);
            sendError(session, "处理消息失败，请重试");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long interviewId = sessionInterviewMap.remove(session.getId());
        if (interviewId != null) {
            log.info("面试 WebSocket 连接关闭: interviewId={}, status={}", interviewId, status);
            // 持久化当前对话（如果还在进行中）
            InterviewContext context = contextStore.get(interviewId);
            if (context != null) {
                persistConversation(interviewId, context);
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket 传输错误: sessionId={}", session.getId(), exception);
        Long interviewId = sessionInterviewMap.remove(session.getId());
        if (interviewId != null) {
            InterviewContext context = contextStore.get(interviewId);
            if (context != null) {
                persistConversation(interviewId, context);
            }
        }
    }

    private void persistConversation(Long interviewId, InterviewContext context) {
        try {
            interviewRepository.findById(interviewId).ifPresent(interview -> {
                List<Map<String, Object>> msgList = context.getMessages().stream()
                        .map(msg -> {
                            Map<String, Object> map = new HashMap<>();
                            map.put("type", msg.getType());
                            map.put("content", msg.getContent());
                            map.put("timestamp", msg.getTimestamp());
                            return map;
                        })
                        .toList();

                interview.setConversation(Map.of("messages", msgList));
                interviewRepository.save(interview);
            });
        } catch (Exception e) {
            log.error("持久化对话记录失败: interviewId={}", interviewId, e);
        }
    }

    private void sendMessage(WebSocketSession session, InterviewMessage message) throws IOException {
        if (session.isOpen()) {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
        }
    }

    private void sendError(WebSocketSession session, String error) {
        try {
            InterviewMessage errMsg = InterviewMessage.builder()
                    .type("system")
                    .content(error)
                    .timestamp(now())
                    .metadata(Map.of("error", true))
                    .build();
            sendMessage(session, errMsg);
        } catch (IOException e) {
            log.error("发送错误消息失败", e);
        }
    }

    private Long extractInterviewId(String uri) {
        try {
            // URI pattern: /ws/interview/{id}?token=xxx
            String path = UriComponentsBuilder.fromUriString(uri).build().getPath();
            if (path != null) {
                String[] segments = path.split("/");
                // segments: ["", "ws", "interview", "{id}"]
                for (int i = 0; i < segments.length; i++) {
                    if ("interview".equals(segments[i]) && i + 1 < segments.length) {
                        return Long.parseLong(segments[i + 1]);
                    }
                }
            }
        } catch (NumberFormatException e) {
            log.error("解析面试ID失败: uri={}", uri);
        }
        return null;
    }

    private String extractToken(String uri) {
        try {
            var queryParams = UriComponentsBuilder.fromUriString(uri).build().getQueryParams();
            return queryParams.getFirst("token");
        } catch (Exception e) {
            return null;
        }
    }

    private String now() {
        return LocalDateTime.now().format(TIMESTAMP_FMT);
    }
}
