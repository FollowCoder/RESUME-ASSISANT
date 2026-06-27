package com.resume.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.agent.interviewer.InterviewContext;
import com.resume.agent.interviewer.InterviewContextStore;
import com.resume.model.dto.InterviewMessage;
import com.resume.model.entity.Interview;
import com.resume.repository.InterviewRepository;
import com.resume.service.InterviewAgentService;
import com.resume.service.SpeechService;
import com.resume.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;

/**
 * 语音面试 WebSocket 处理器 - 处理语音模式面试的 WebSocket 连接
 *
 * 消息协议（前端→后端）：
 * - Text frame: JSON { type: "start_recording" | "stop_recording" | "text", content?: string }
 * - Binary frame: PCM 音频数据（录音中持续发送）
 *
 * 消息协议（后端→前端）：
 * - Text frame: JSON { type: "candidate_text" | "interviewer_text" | "system", content: string }
 * - Binary frame: TTS 音频数据（面试官回复的语音）
 */
@Component
public class SpeechWebSocketHandler extends AbstractWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(SpeechWebSocketHandler.class);
    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SpeechService speechService;
    private final InterviewAgentService interviewAgentService;
    private final InterviewContextStore contextStore;
    private final InterviewRepository interviewRepository;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    // session -> interviewId 映射
    private final Map<String, Long> sessionInterviewMap = new ConcurrentHashMap<>();
    // session -> 发送锁（保证同一 session 的 send 操作串行化，避免 TEXT_PARTIAL_WRITING 冲突）
    private final Map<String, Object> sessionSendLocks = new ConcurrentHashMap<>();
    // session -> 录音音频缓冲区（用于保存到本地文件）
    private final Map<String, ByteArrayOutputStream> sessionAudioBuffers = new ConcurrentHashMap<>();

    @Value("${app.speech.recording-dir:./audio-recordings}")
    private String recordingDir;

    public SpeechWebSocketHandler(SpeechService speechService,
                                   InterviewAgentService interviewAgentService,
                                   InterviewContextStore contextStore,
                                   InterviewRepository interviewRepository,
                                   JwtUtil jwtUtil,
                                   ObjectMapper objectMapper) {
        this.speechService = speechService;
        this.interviewAgentService = interviewAgentService;
        this.contextStore = contextStore;
        this.interviewRepository = interviewRepository;
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        try {
            String uri = session.getUri().toString();
            Long interviewId = extractInterviewId(uri);
            String token = extractToken(uri);

            if (interviewId == null) {
                sendTextMessage(session, "system", "无效的面试ID");
                session.close(CloseStatus.BAD_DATA);
                return;
            }

            // 验证 token
            Long userId = null;
            if (token != null && jwtUtil.isTokenValid(token)) {
                userId = jwtUtil.extractUserId(token);
            }
            if (userId == null) {
                sendTextMessage(session, "system", "身份验证失败，请重新登录");
                session.close(CloseStatus.POLICY_VIOLATION);
                return;
            }

            // 验证面试记录
            Optional<Interview> interviewOpt = interviewRepository.findById(interviewId);
            if (interviewOpt.isEmpty()) {
                sendTextMessage(session, "system", "面试记录不存在");
                session.close(CloseStatus.BAD_DATA);
                return;
            }

            Interview interview = interviewOpt.get();
            if (!interview.getUserId().equals(userId)) {
                sendTextMessage(session, "system", "无权访问该面试");
                session.close(CloseStatus.POLICY_VIOLATION);
                return;
            }

            // 记录映射
            sessionInterviewMap.put(session.getId(), interviewId);

            // 获取面试上下文
            InterviewContext context = contextStore.get(interviewId);
            if (context == null) {
                sendTextMessage(session, "system", "面试上下文不存在，请先通过API启动面试");
                session.close(CloseStatus.BAD_DATA);
                return;
            }

            // 发送系统消息
            sendTextMessage(session, "system", "语音面试连接已建立，面试即将开始...");

            // 生成面试官开场白
            String opening = interviewAgentService.generateOpening(String.valueOf(interviewId), context);

            InterviewMessage openingMsg = InterviewMessage.builder()
                    .type("interviewer")
                    .content(opening)
                    .timestamp(now())
                    .metadata(Map.of("stage", context.getCurrentStage().name()))
                    .build();
            context.addMessage(openingMsg);

            // 发送开场白文本
            sendTextMessage(session, "interviewer_text", opening);

            // TTS 合成开场白语音并流式发送
            synthesizeAndSend(session, opening);

            log.info("语音面试 WebSocket 连接建立: interviewId={}, userId={}", interviewId, userId);

        } catch (Exception e) {
            log.error("语音 WebSocket 连接建立失败", e);
            sendTextMessage(session, "system", "连接建立失败: " + e.getMessage());
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long interviewId = sessionInterviewMap.get(session.getId());
        if (interviewId == null) {
            sendTextMessage(session, "system", "会话未建立面试关联");
            return;
        }

        InterviewContext context = contextStore.get(interviewId);
        if (context == null) {
            sendTextMessage(session, "system", "面试上下文已失效");
            return;
        }

        try {
            JsonNode msgNode = objectMapper.readTree(message.getPayload());
            String type = msgNode.path("type").asText("");

            switch (type) {
                case "start_recording":
                    handleStartRecording(session);
                    break;
                case "stop_recording":
                    handleStopRecording(session, context);
                    break;
                case "text":
                    // 直接文本输入（fallback）
                    String content = msgNode.path("content").asText("");
                    if (!content.isBlank()) {
                        processInterviewerReply(session, content, context, interviewId);
                    }
                    break;
                default:
                    sendTextMessage(session, "system", "未知的消息类型: " + type);
            }
        } catch (Exception e) {
            log.error("处理语音面试消息失败: interviewId={}", interviewId, e);
            sendTextMessage(session, "system", "处理消息失败，请重试");
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        byte[] audioData = message.getPayload().array();

        // 保存音频数据到本地缓冲区（用于后续文件转写）
        ByteArrayOutputStream buffer = sessionAudioBuffers.get(session.getId());
        if (buffer != null) {
            buffer.write(audioData);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long interviewId = sessionInterviewMap.remove(session.getId());
        // 清理发送锁
        sessionSendLocks.remove(session.getId());
        // 清理并保存音频缓冲区
        closeAndSaveAudioBuffer(session.getId());

        if (interviewId != null) {
            log.info("语音面试 WebSocket 连接关闭: interviewId={}, status={}", interviewId, status);
            InterviewContext context = contextStore.get(interviewId);
            if (context != null) {
                persistConversation(interviewId, context);
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("语音 WebSocket 传输错误: sessionId={}", session.getId(), exception);
        Long interviewId = sessionInterviewMap.remove(session.getId());
        // 清理发送锁
        sessionSendLocks.remove(session.getId());
        // 清理并保存音频缓冲区
        closeAndSaveAudioBuffer(session.getId());
        if (interviewId != null) {
            InterviewContext context = contextStore.get(interviewId);
            if (context != null) {
                persistConversation(interviewId, context);
            }
        }
    }

    // =========================================================================
    // 内部方法
    // =========================================================================

    private void handleStartRecording(WebSocketSession session) {
        // 创建音频缓冲区用于本地保存
        sessionAudioBuffers.put(session.getId(), new ByteArrayOutputStream());

        // 通知前端录音开始
        try {
            sendTextMessage(session, "system", "录音开始，请说话...");
        } catch (IOException e) {
            log.error("发送消息失败", e);
        }

        log.debug("录音开始: sessionId={}", session.getId());
    }

    private void handleStopRecording(WebSocketSession session, InterviewContext context) {
        Long interviewId = sessionInterviewMap.get(session.getId());

        // 保存录音文件到本地
        ByteArrayOutputStream buffer = sessionAudioBuffers.remove(session.getId());
        String audioFilePath = saveAudioToFile(session.getId(), buffer);

        if (audioFilePath == null) {
            try {
                sendTextMessage(session, "system", "录音保存失败，请重新录音");
            } catch (IOException e) {
                log.error("发送消息失败", e);
            }
            return;
        }

        // 通知录音保存路径
        try {
            sendTextMessage(session, "system", "录音已保存，正在识别语音内容...");
        } catch (IOException e) {
            log.error("发送消息失败", e);
        }

        // 使用文件转写 API 进行语音识别（异步执行，避免阻塞 WebSocket 线程）
        speechService.transcribeFileAsync(audioFilePath)
                .thenAccept(transcript -> {
                    if (transcript == null || transcript.isBlank()) {
                        try {
                            sendTextMessage(session, "system",
                                    "未识别到语音内容，请重新录音（录音已保存至: " + audioFilePath + "）");
                        } catch (IOException e) {
                            log.error("发送消息失败", e);
                        }
                        return;
                    }

                    // 通知识别完成
                    try {
                        sendTextMessage(session, "system", "语音识别完成");
                    } catch (IOException e) {
                        log.error("发送消息失败", e);
                    }

                    // 处理面试官回复
                    processInterviewerReply(session, transcript, context, interviewId);
                })
                .exceptionally(ex -> {
                    log.error("文件转写失败: {}", audioFilePath, ex);
                    try {
                        sendTextMessage(session, "system",
                                "语音识别失败，请重新录音（录音已保存至: " + audioFilePath + "）");
                    } catch (IOException e) {
                        log.error("发送消息失败", e);
                    }
                    return null;
                });
    }

    private void processInterviewerReply(WebSocketSession session, String candidateText,
                                          InterviewContext context, Long interviewId) {
        // 记录候选人消息
        InterviewMessage candidateMsg = InterviewMessage.builder()
                .type("candidate")
                .content(candidateText)
                .timestamp(now())
                .build();
        context.addMessage(candidateMsg);

        // 检查是否面试结束
        if (context.getCurrentStage() == InterviewContext.Stage.COMPLETED) {
            try {
                sendTextMessage(session, "system", "面试已结束，请通过API获取面试报告。");
            } catch (IOException e) {
                log.error("发送消息失败", e);
            }
            return;
        }

        // 调用面试官处理回答
        String reply = interviewAgentService.processAnswer(String.valueOf(interviewId), candidateText, context);

        InterviewMessage interviewerMsg = InterviewMessage.builder()
                .type("interviewer")
                .content(reply)
                .timestamp(now())
                .metadata(Map.of("stage", context.getCurrentStage().name()))
                .build();
        context.addMessage(interviewerMsg);

        try {
            // 发送候选人转写文本（让前端聊天区显示用户说了什么）
            sendTextMessage(session, "candidate_text", candidateText);
            // 发送面试官文本
            sendTextMessage(session, "interviewer_text", reply);

            // TTS 合成并流式发送音频
            synthesizeAndSend(session, reply);

            // 如果面试结束
            if (context.getCurrentStage() == InterviewContext.Stage.COMPLETED) {
                sendTextMessage(session, "system", "面试已结束，正在生成评估报告...");
                persistConversation(interviewId, context);
            }
        } catch (IOException e) {
            log.error("发送面试官回复失败", e);
        }
    }

    private void synthesizeAndSend(WebSocketSession session, String text) {
        Object lock = sessionSendLocks.computeIfAbsent(session.getId(), k -> new Object());
        speechService.synthesize(text,
                // 音频 chunk 回调
                audioChunk -> {
                    synchronized (lock) {
                        if (session.isOpen()) {
                            try {
                                session.sendMessage(new BinaryMessage(ByteBuffer.wrap(audioChunk)));
                            } catch (IOException e) {
                                log.error("发送 TTS 音频失败", e);
                            } catch (IllegalStateException e) {
                                log.debug("发送 TTS 音频时 session 状态异常: sessionId={}", session.getId());
                            }
                        }
                    }
                },
                // 合成完成回调
                () -> {
                    if (session.isOpen()) {
                        try {
                            sendTextMessage(session, "tts_complete", "");
                        } catch (IOException e) {
                            log.debug("发送 TTS 完成标记失败", e);
                        }
                    }
                }
        );
    }

    private void sendTextMessage(WebSocketSession session, String type, String content) throws IOException {
        if (!session.isOpen()) {
            return;
        }
        Object lock = sessionSendLocks.computeIfAbsent(session.getId(), k -> new Object());
        synchronized (lock) {
            if (!session.isOpen()) {
                return;
            }
            try {
                Map<String, String> msg = Map.of("type", type, "content", content);
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(msg)));
            } catch (IllegalStateException e) {
                log.warn("发送文本消息失败，session 状态异常: sessionId={}, type={}", session.getId(), type);
                // 状态异常时忽略，通常是 session 正在关闭或并发写入冲突
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

    private Long extractInterviewId(String uri) {
        try {
            String path = UriComponentsBuilder.fromUriString(uri).build().getPath();
            if (path != null) {
                // URI pattern: /ws/interview/{id}/voice
                String[] segments = path.split("/");
                for (int i = 0; i < segments.length; i++) {
                    if ("interview".equals(segments[i]) && i + 1 < segments.length) {
                        String idStr = segments[i + 1];
                        // 跳过 "voice" 等非数字段
                        return Long.parseLong(idStr);
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

    // =========================================================================
    // 本地录音保存
    // =========================================================================

    /**
     * 将音频缓冲区写入 WAV 文件
     * @return 文件绝对路径，失败返回 null
     */
    private String saveAudioToFile(String sessionId, ByteArrayOutputStream buffer) {
        if (buffer == null || buffer.size() == 0) {
            return null;
        }
        try {
            Path dir = Paths.get(recordingDir);
            Files.createDirectories(dir);
            String filename = "recording_" + sessionId.substring(0, Math.min(8, sessionId.length())) + "_"
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".wav";
            Path filePath = dir.resolve(filename);

            byte[] pcmData = buffer.toByteArray();
            byte[] wavData = createWavFile(pcmData, 16000, 1, 16);

            Files.write(filePath, wavData);

            log.info("录音文件已保存: {}", filePath.toAbsolutePath());
            return filePath.toAbsolutePath().toString();
        } catch (IOException e) {
            log.error("保存录音文件失败: sessionId={}", sessionId, e);
            return null;
        }
    }

    /**
     * 为原始 PCM 数据添加 WAV 文件头
     */
    private byte[] createWavFile(byte[] pcmData, int sampleRate, int channels, int bitsPerSample) {
        int byteRate = sampleRate * channels * bitsPerSample / 8;
        int blockAlign = channels * bitsPerSample / 8;
        int dataSize = pcmData.length;
        int totalSize = 36 + dataSize;

        ByteBuffer buffer = ByteBuffer.allocate(44 + dataSize);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // RIFF 头
        buffer.put("RIFF".getBytes());
        buffer.putInt(totalSize);
        buffer.put("WAVE".getBytes());

        // fmt 子块
        buffer.put("fmt ".getBytes());
        buffer.putInt(16);          // 子块大小
        buffer.putShort((short) 1); // PCM 格式
        buffer.putShort((short) channels);
        buffer.putInt(sampleRate);
        buffer.putInt(byteRate);
        buffer.putShort((short) blockAlign);
        buffer.putShort((short) bitsPerSample);

        // data 子块
        buffer.put("data".getBytes());
        buffer.putInt(dataSize);
        buffer.put(pcmData);

        return buffer.array();
    }

    /**
     * 清理音频缓冲区（丢弃，不保存）
     */
    private void closeAudioBuffer(String sessionId) {
        ByteArrayOutputStream buffer = sessionAudioBuffers.remove(sessionId);
        if (buffer != null) {
            try {
                buffer.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * 清理并尝试保存音频缓冲区（连接异常断开时使用）
     */
    private void closeAndSaveAudioBuffer(String sessionId) {
        ByteArrayOutputStream buffer = sessionAudioBuffers.remove(sessionId);
        if (buffer != null) {
            String path = saveAudioToFile(sessionId, buffer);
            if (path != null) {
                log.info("异常断开时录音已保存: {}", path);
            }
        }
    }
}
