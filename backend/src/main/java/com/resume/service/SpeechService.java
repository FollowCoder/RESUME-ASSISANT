package com.resume.service;

import com.alibaba.dashscope.audio.asr.transcription.Transcription;
import com.alibaba.dashscope.audio.asr.transcription.TranscriptionParam;
import com.alibaba.dashscope.audio.asr.transcription.TranscriptionQueryParam;
import com.alibaba.dashscope.audio.asr.transcription.TranscriptionResult;
import com.alibaba.dashscope.audio.asr.transcription.TranscriptionTaskResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * 语音服务 - 集成通义语音 ASR（语音转文字）和 TTS/CosyVoice（文字转语音）
 * <p>
 * ASR 提供两种模式：
 * <ul>
 *   <li>实时流式（WebSocket）：fun-asr-realtime 模型，用于面试场景即时转写</li>
 *   <li>文件转写（HTTP）：fun-asr 模型，用于离线音频文件的批量转写</li>
 * </ul>
 */
@Service
public class SpeechService {

    private static final Logger log = LoggerFactory.getLogger(SpeechService.class);

    private static final String SPEECH_WS_URL = "wss://dashscope.aliyuncs.com/api-ws/v1/inference";

    @Value("${agentscope.llm.api-key}")
    private String apiKey;

    /** 实时 ASR 模型（WebSocket 流式），默认 fun-asr-realtime（替代旧版 paraformer-realtime-v2） */
    @Value("${app.speech.asr-model:fun-asr}")
    private String asrModel;

    /** 文件转写模型（DashScope SDK），默认 fun-asr */
    @Value("${app.speech.transcription-model:fun-asr}")
    private String transcriptionModel;

    @Value("${app.speech.tts-model:cosyvoice-v1}")
    private String ttsModel;

    @Value("${app.speech.tts-voice:longxiaochun}")
    private String ttsVoice;

    private final ObjectMapper objectMapper;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public SpeechService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PreDestroy
    public void destroy() {
        executorService.shutdownNow();
    }

    // =========================================================================
    // ASR (语音转文字) - 实时流式转写
    // =========================================================================

    /**
     * 创建 ASR WebSocket 会话，返回会话对象用于流式发送音频
     *
     * @param onPartialResult 实时部分转写结果回调
     * @param onFinalResult   最终转写结果回调
     * @return ASR 会话
     */
    public AsrSession createAsrSession(Consumer<String> onPartialResult, Consumer<String> onFinalResult) {
        return new AsrSession(onPartialResult, onFinalResult);
    }

    /**
     * ASR 会话 - 管理一次完整的实时语音转文字过程
     * <p>
     * 协议遵循 DashScope WebSocket 流式 ASR 规范：
     * 建立连接 → run-task → 发送音频 → 接收 result-generated → finish-task
     */
    public class AsrSession {
        private WebSocketSession wsSession;
        private final Consumer<String> onPartialResult;
        private final Consumer<String> onFinalResult;
        private final String taskId;
        private final CompletableFuture<Void> connectionReady = new CompletableFuture<>();
        private volatile boolean started = false;
        private final StringBuilder fullTranscript = new StringBuilder();

        AsrSession(Consumer<String> onPartialResult, Consumer<String> onFinalResult) {
            this.onPartialResult = onPartialResult;
            this.onFinalResult = onFinalResult;
            this.taskId = UUID.randomUUID().toString().replace("-", "");
        }

        /**
         * 开始 ASR 会话 - 建立连接并发送 run-task 事件
         */
        public void start() {
            executorService.submit(() -> {
                try {
                    StandardWebSocketClient client = new StandardWebSocketClient();
                    WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
                    headers.add("Authorization", "Bearer " + apiKey);

                    wsSession = client.execute(new WebSocketHandler() {
                        @Override
                        public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                            // 发送 run-task 事件
                            ObjectNode message = buildAsrRunTaskMessage();
                            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
                        }

                        @Override
                        public void handleMessage(WebSocketSession session, WebSocketMessage<?> msg) throws Exception {
                            if (msg instanceof TextMessage textMsg) {
                                handleAsrTextMessage(textMsg.getPayload());
                            }
                        }

                        @Override
                        public void handleTransportError(WebSocketSession session, Throwable exception) {
                            log.error("ASR WebSocket 传输错误", exception);
                            connectionReady.completeExceptionally(exception);
                        }

                        @Override
                        public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
                            log.debug("ASR WebSocket 连接关闭: {}", closeStatus);
                        }

                        @Override
                        public boolean supportsPartialMessages() {
                            return false;
                        }
                    }, headers, URI.create(SPEECH_WS_URL)).get(10, TimeUnit.SECONDS);

                } catch (Exception e) {
                    log.error("ASR 连接建立失败", e);
                    connectionReady.completeExceptionally(e);
                }
            });
        }

        /**
         * 发送音频数据
         */
        public void sendAudio(byte[] audioData) {
            if (wsSession != null && wsSession.isOpen() && started) {
                try {
                    wsSession.sendMessage(new BinaryMessage(ByteBuffer.wrap(audioData)));
                } catch (Exception e) {
                    log.error("发送音频数据失败", e);
                }
            }
        }

        /**
         * 结束 ASR 会话 - 发送 finish-task 事件
         */
        public String finish() {
            if (wsSession != null && wsSession.isOpen()) {
                try {
                    ObjectNode finishMessage = objectMapper.createObjectNode();
                    ObjectNode header = finishMessage.putObject("header");
                    header.put("task_id", taskId);
                    header.put("action", "finish-task");
                    header.put("streaming", "duplex");
                    finishMessage.putObject("payload").putObject("input");

                    wsSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(finishMessage)));

                    // 等待一小段时间让最终结果返回
                    Thread.sleep(500);

                    wsSession.close();
                } catch (Exception e) {
                    log.error("结束 ASR 会话失败", e);
                }
            }
            return fullTranscript.toString();
        }

        /**
         * 强制关闭
         */
        public void close() {
            if (wsSession != null && wsSession.isOpen()) {
                try {
                    wsSession.close();
                } catch (Exception e) {
                    log.debug("关闭 ASR 会话异常", e);
                }
            }
        }

        /**
         * 构建 run-task 消息（启动识别任务）
         * <p>
         * 协议参考：<a href="https://help.aliyun.com/zh/model-studio/fun-asr-recorded-speech-recognition-java-sdk?spm=a2c4g.11186623.help-menu-2400256.d_2_5_2_3_2.4316559dS312d6&scm=20140722.H_2978299._.OR_help-T_cn~zh-V_1">客户端事件</a>
         */
        private ObjectNode buildAsrRunTaskMessage() {
            ObjectNode message = objectMapper.createObjectNode();

            // header - 任务控制
            ObjectNode header = message.putObject("header");
            header.put("task_id", taskId);
            header.put("action", "run-task");
            header.put("streaming", "duplex");

            // payload - 模型与参数配置
            ObjectNode payload = message.putObject("payload");
            payload.put("task_group", "audio");
            payload.put("task", "asr");
            payload.put("function", "recognition");
            payload.put("model", asrModel);

            ObjectNode parameters = payload.putObject("parameters");
            parameters.putArray("language_hints").add("zh");
            parameters.put("sample_rate", 16000);
            parameters.put("format", "pcm");

            // input 占位（音频通过 Binary frame 发送）
            payload.putObject("input");

            return message;
        }

        /**
         * 处理 ASR 服务端返回的文本消息
         * <p>
         * 事件：task-started → result-generated（可多次） → task-finished / task-failed
         */
        private void handleAsrTextMessage(String payload) {
            try {
                JsonNode root = objectMapper.readTree(payload);
                JsonNode header = root.path("header");
                String event = header.path("event").asText("");

                switch (event) {
                    case "task-started":
                        started = true;
                        connectionReady.complete(null);
                        log.debug("ASR 任务已启动: taskId={}, model={}", taskId, asrModel);
                        break;

                    case "result-generated":
                        JsonNode output = root.path("payload").path("output");
                        JsonNode sentence = output.path("sentence");
                        if (!sentence.isMissingNode()) {
                            String text = sentence.path("text").asText("");
                            boolean isEnd = sentence.has("end_time")
                                    && !sentence.path("end_time").asText().isEmpty();

                            if (!text.isEmpty()) {
                                if (onPartialResult != null) {
                                    onPartialResult.accept(text);
                                }
                                if (isEnd) {
                                    fullTranscript.append(text);
                                }
                            }
                        }
                        break;

                    case "task-finished":
                        log.debug("ASR 任务已完成: taskId={}", taskId);
                        if (onFinalResult != null && !fullTranscript.isEmpty()) {
                            onFinalResult.accept(fullTranscript.toString());
                        }
                        break;

                    case "task-failed":
                        String errorMsg = header.path("error_message").asText("ASR 任务失败");
                        log.error("ASR 任务失败: {}", errorMsg);
                        connectionReady.completeExceptionally(new RuntimeException(errorMsg));
                        break;

                    default:
                        log.debug("ASR 收到未知事件: {}", event);
                }
            } catch (Exception e) {
                log.error("处理 ASR 响应消息失败", e);
            }
        }

        public boolean isStarted() {
            return started;
        }

        public CompletableFuture<Void> getConnectionReady() {
            return connectionReady;
        }
    }

    // =========================================================================
    // 文件转写 - 使用 DashScope SDK TranscriptionParam.builder() 模式
    // =========================================================================

    /**
     * 转写本地音频文件（Base64 编码方式）
     * <p>
     * 使用 DashScope 同步调用 API，通过 Base64 编码上传音频数据。
     * 适用于本地文件，无需公网 URL。
     *
     * @param localFilePath 本地音频文件绝对路径
     * @param languageHints 语言提示，如 {@code "zh"}, {@code "en"}
     * @return 转写文本，失败返回 null
     */
    public String transcribeFile(String localFilePath, String... languageHints) {
        Path filePath = Path.of(localFilePath);
        try {
            byte[] audioBytes = Files.readAllBytes(filePath);
            String format = detectAudioFormat(localFilePath);
            log.info("开始转写文件: {}, 格式: {}", localFilePath, format);

            // 使用同步调用 API（Base64 编码）
            return doTranscribeSync(audioBytes, format, languageHints);
        } catch (Exception e) {
            log.error("文件转写失败: {}", localFilePath, e);
            return null;
        } finally {
            // 转写结束后删除语音文件
            try {
                Files.deleteIfExists(filePath);
                log.debug("已删除语音文件: {}", localFilePath);
            } catch (Exception e) {
                log.warn("删除语音文件失败: {}", localFilePath, e);
            }
        }
    }

    /**
     * 转写公网可访问的音频文件 URL
     *
     * @param audioUrl      公网可访问的音频文件 URL（需支持 HTTP GET）
     * @param languageHints 语言提示
     * @return 转写文本，失败返回 null
     */
    public String transcribeUrl(String audioUrl, String... languageHints) {
        try {
            TranscriptionParam param = TranscriptionParam.builder()
                    .apiKey(apiKey)
                    .model(transcriptionModel)
                    .fileUrls(List.of(audioUrl))
                    .parameter("language_hints",
                            languageHints != null && languageHints.length > 0
                                    ? languageHints : new String[]{"zh"})
                    .build();
            return doTranscribe(param);
        } catch (Exception e) {
            log.error("URL 转写失败: {}", audioUrl, e);
            return null;
        }
    }

    /**
     * 异步转写本地文件
     */
    public CompletableFuture<String> transcribeFileAsync(String localFilePath) {
        return CompletableFuture.supplyAsync(
                () -> transcribeFile(localFilePath, "zh", "en"), executorService);
    }

    // ---- 内部：SDK 调用与结果解析 ----

    /**
     * 使用 DashScope 同步调用 API 进行文件转写（Base64 编码）
     * <p>
     * 参考：<a href="https://help.aliyun.com/zh/model-studio/fun-asr-recorded-speech-recognition-http-api">录音文件识别 HTTP API</a>
     *
     * @param audioBytes   音频文件字节数据
     * @param format       音频格式（wav, mp3, opus 等）
     * @param languageHints 语言提示
     * @return 转写文本，失败返回 null
     */
    private String doTranscribeSync(byte[] audioBytes, String format, String... languageHints) {
        try {
            // 构建 Base64 编码的音频数据
            String base64Audio = Base64.getEncoder().encodeToString(audioBytes);
            String mimeType = "audio/" + format;
            String dataUri = "data:" + mimeType + ";base64," + base64Audio;

            // 构建请求体
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", "fun-asr-realtime");

            // input.messages
            ObjectNode input = requestBody.putObject("input");
            ArrayNode messages = input.putArray("messages");
            ObjectNode message = messages.addObject();
            message.put("role", "user");
            ArrayNode content = message.putArray("content");
            ObjectNode contentItem = content.addObject();
            contentItem.put("audio", dataUri);

            // parameters
            ObjectNode parameters = requestBody.putObject("parameters");
            parameters.put("format", format);
            if (languageHints != null && languageHints.length > 0) {
                parameters.putArray("language_hints").add(languageHints[0]);
            }

            // 发送 HTTP 请求
            URL url = new URL("https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("X-DashScope-SSE", "disable");
            connection.setDoOutput(true);

            try (var os = connection.getOutputStream()) {
                os.write(objectMapper.writeValueAsBytes(requestBody));
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                log.error("同步转写请求失败: responseCode={}", responseCode);
                // 读取错误信息
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream()))) {
                    String line;
                    StringBuilder errorResponse = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    log.error("错误响应: {}", errorResponse.toString());
                }
                return null;
            }

            // 读取响应
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()))) {
                JsonNode root = objectMapper.readTree(reader);
                return parseSyncTranscriptionResponse(root);
            } finally {
                connection.disconnect();
            }
        } catch (Exception e) {
            log.error("同步转写调用失败", e);
            return null;
        }
    }

    /**
     * 使用 DashScope SDK 执行异步转写（asyncCall + wait 阻塞模式）
     * <p>
     * 适用于公网可访问的文件 URL
     */
    private String doTranscribe(TranscriptionParam param) {
        try {
            Transcription transcription = new Transcription();
            // 提交转写请求
            TranscriptionResult result = transcription.asyncCall(param);
            log.info("转写任务已提交: taskId={}, requestId={}",
                    result.getTaskId(), result.getRequestId());

            // 阻塞等待任务完成并获取结果
            TranscriptionQueryParam queryParam =
                    TranscriptionQueryParam.FromTranscriptionParam(param, result.getTaskId());
            result = transcription.wait(queryParam);

            // 获取转写结果
            List<TranscriptionTaskResult> taskResultList = result.getResults();
            if (taskResultList != null && !taskResultList.isEmpty()) {
                for (TranscriptionTaskResult taskResult : taskResultList) {
                    String transcriptionUrl = taskResult.getTranscriptionUrl();
                    if (transcriptionUrl != null && !transcriptionUrl.isEmpty()) {
                        return fetchTranscriptionText(transcriptionUrl);
                    }
                }
            }
            log.warn("转写完成但未获取到结果文本");
            return null;
        } catch (Exception e) {
            log.error("SDK 转写调用失败", e);
            return null;
        }
    }

    /**
     * 从转写结果 URL 下载并解析文本
     * <p>
     * 结果 JSON 格式：
     * <pre>{@code
     * {"transcripts": [{"channel_id": 0, "sentences": [
     *   {"text": "您好", "begin_time": 0, "end_time": 1000}
     * ]}]}
     * }</pre>
     */
    private String fetchTranscriptionText(String transcriptionUrl) {
        try {
            HttpURLConnection connection =
                    (HttpURLConnection) new URL(transcriptionUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()))) {
                JsonNode root = objectMapper.readTree(reader);
                return parseTranscriptionJson(root);
            } finally {
                connection.disconnect();
            }
        } catch (Exception e) {
            log.error("获取转写文本失败: {}", transcriptionUrl, e);
            return null;
        }
    }

    /** 从转写结果 JSON 中提取 sentences 拼接为全文 */
    private String parseTranscriptionJson(JsonNode root) {
        JsonNode transcripts = root.path("transcripts");
        if (transcripts.isArray() && !transcripts.isEmpty()) {
            JsonNode sentences = transcripts.get(0).path("sentences");
            if (sentences.isArray()) {
                StringBuilder sb = new StringBuilder();
                for (JsonNode s : sentences) {
                    String text = s.path("text").asText("");
                    if (!text.isEmpty()) {
                        if (!sb.isEmpty()) sb.append(" ");
                        sb.append(text);
                    }
                }
                log.info("转写结果: {}", sb.toString());
                return sb.isEmpty() ? null : sb.toString();
            }
        }
        return null;
    }

    /**
     * 解析同步转写 API 的响应
     * <p>
     * 响应格式：
     * <pre>{@code
     * {
     *   "output": {
     *     "text": "识别文本",
     *     "sentence": { ... }
     *   },
     *   "usage": { "duration": 2 },
     *   "request_id": "..."
     * }
     * }</pre>
     */
    private String parseSyncTranscriptionResponse(JsonNode root) {
        JsonNode output = root.path("output");
        if (output.isMissingNode()) {
            log.warn("同步转写响应缺少 output 字段");
            return null;
        }

        // 优先使用 output.text（完整识别文本）
        String text = output.path("text").asText("");
        if (!text.isEmpty()) {
            log.info("同步转写结果: {}", text);
            return text;
        }

        // 备选：从 sentence.text 获取
        JsonNode sentence = output.path("sentence");
        if (!sentence.isMissingNode()) {
            String sentenceText = sentence.path("text").asText("");
            if (!sentenceText.isEmpty()) {
                log.info("同步转写结果（句子）: {}", sentenceText);
                return sentenceText;
            }
        }

        log.warn("同步转写响应中未找到识别文本");
        return null;
    }

    /** 根据扩展名推断音频格式 */
    private String detectAudioFormat(String filePath) {
        String name = filePath.toLowerCase();
        if (name.endsWith(".mp3")) return "mp3";
        if (name.endsWith(".opus")) return "opus";
        if (name.endsWith(".aac")) return "aac";
        if (name.endsWith(".flac")) return "flac";
        if (name.endsWith(".ogg")) return "ogg";
        if (name.endsWith(".amr")) return "amr";
        if (name.endsWith(".m4a")) return "m4a";
        return "wav";
    }

    // =========================================================================
    // TTS (文字转语音) - CosyVoice
    // =========================================================================

    /**
     * 将文本合成为语音，通过回调流式返回音频数据
     *
     * @param text         待合成的文本
     * @param onAudioChunk 音频数据块回调（PCM/WAV格式）
     * @param onComplete   合成完成回调
     */
    public void synthesize(String text, Consumer<byte[]> onAudioChunk, Runnable onComplete) {
        executorService.submit(() -> {
            try {
                String taskId = UUID.randomUUID().toString().replace("-", "");
                StandardWebSocketClient client = new StandardWebSocketClient();
                WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
                headers.add("Authorization", "Bearer " + apiKey);

                CompletableFuture<Void> taskStarted = new CompletableFuture<>();

                WebSocketSession session = client.execute(new WebSocketHandler() {
                    @Override
                    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                        // 发送 run-task 事件
                        ObjectNode message = buildTtsRunTaskMessage(taskId);
                        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
                    }

                    @Override
                    public void handleMessage(WebSocketSession session, WebSocketMessage<?> msg) throws Exception {
                        if (msg instanceof TextMessage textMsg) {
                            handleTtsTextMessage(textMsg.getPayload(), taskId, taskStarted, session, text);
                        } else if (msg instanceof BinaryMessage binaryMsg) {
                            // 收到音频数据
                            byte[] audioData = binaryMsg.getPayload().array();
                            if (onAudioChunk != null) {
                                onAudioChunk.accept(audioData);
                            }
                        }
                    }

                    @Override
                    public void handleTransportError(WebSocketSession session, Throwable exception) {
                        log.error("TTS WebSocket 传输错误", exception);
                        taskStarted.completeExceptionally(exception);
                    }

                    @Override
                    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
                        log.debug("TTS WebSocket 连接关闭: {}", closeStatus);
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    }

                    @Override
                    public boolean supportsPartialMessages() {
                        return false;
                    }
                }, headers, URI.create(SPEECH_WS_URL)).get(10, TimeUnit.SECONDS);

            } catch (Exception e) {
                log.error("TTS 合成失败", e);
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }

    private ObjectNode buildTtsRunTaskMessage(String taskId) {
        ObjectNode message = objectMapper.createObjectNode();
        ObjectNode header = message.putObject("header");
        header.put("task_id", taskId);
        header.put("action", "run-task");
        header.put("streaming", "duplex");

        ObjectNode payload = message.putObject("payload");
        payload.put("task_group", "audio");
        payload.put("task", "tts");
        payload.put("function", "SpeechSynthesizer");
        payload.put("model", ttsModel);
        ObjectNode parameters = payload.putObject("parameters");
        parameters.put("text_type", "PlainText");
        parameters.put("voice", ttsVoice);
        parameters.put("format", "wav");
        parameters.put("sample_rate", 22050);
        payload.putObject("input");

        return message;
    }

    private void handleTtsTextMessage(String payload, String taskId,
                                       CompletableFuture<Void> taskStarted,
                                       WebSocketSession session, String text) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode header = root.path("header");
            String event = header.path("event").asText("");

            switch (event) {
                case "task-started":
                    log.debug("TTS 任务已启动: taskId={}", taskId);
                    taskStarted.complete(null);
                    // 发送 continue-task 传入要合成的文本
                    sendTtsContinueTask(session, taskId, text);
                    // 发送 finish-task 结束输入
                    sendTtsFinishTask(session, taskId);
                    break;
                case "task-finished":
                    log.debug("TTS 任务已完成: taskId={}", taskId);
                    try {
                        session.close();
                    } catch (Exception e) {
                        log.debug("关闭 TTS 会话异常", e);
                    }
                    break;
                case "task-failed":
                    String errorMsg = header.path("error_message").asText("TTS 任务失败");
                    log.error("TTS 任务失败: {}", errorMsg);
                    taskStarted.completeExceptionally(new RuntimeException(errorMsg));
                    break;
                default:
                    log.debug("TTS 收到未知事件: {}", event);
            }
        } catch (Exception e) {
            log.error("处理 TTS 响应消息失败", e);
        }
    }

    private void sendTtsContinueTask(WebSocketSession session, String taskId, String text) {
        try {
            ObjectNode message = objectMapper.createObjectNode();
            ObjectNode header = message.putObject("header");
            header.put("task_id", taskId);
            header.put("action", "continue-task");
            header.put("streaming", "duplex");

            ObjectNode payload = message.putObject("payload");
            ObjectNode input = payload.putObject("input");
            input.put("text", text);

            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        } catch (Exception e) {
            log.error("发送 TTS continue-task 失败", e);
        }
    }

    private void sendTtsFinishTask(WebSocketSession session, String taskId) {
        try {
            ObjectNode message = objectMapper.createObjectNode();
            ObjectNode header = message.putObject("header");
            header.put("task_id", taskId);
            header.put("action", "finish-task");
            header.put("streaming", "duplex");
            message.putObject("payload").putObject("input");

            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        } catch (Exception e) {
            log.error("发送 TTS finish-task 失败", e);
        }
    }
}
