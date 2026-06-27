package com.resume.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.agent.AgentFactory;
import com.resume.agent.AgentMemoryPolicy;
import com.resume.agent.writer.ConversationContext;
import com.resume.agent.writer.ConversationStore;
import com.resume.model.dto.ChatMessage;
import com.resume.model.dto.ResumeChatResponse;
import com.resume.model.dto.ResumeContent;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 简历编写服务 - 使用 AgentScope 规范重构
 * 通过多轮对话引导用户完成简历编写
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeAgentService {

    private final AgentFactory agentFactory;
    private final AgentMemoryPolicy memoryPolicy;
    private final ConversationStore conversationStore;
    private final ObjectMapper objectMapper;

    private static final String STAGE_COMPLETE_MARKER = "[STAGE_COMPLETE]";
    private static final String DATA_START_MARKER = "[DATA_START]";
    private static final String DATA_END_MARKER = "[DATA_END]";

    /**
     * 处理用户消息并返回 Agent 回复
     */
    public ResumeChatResponse chat(String sessionId, String userMessage, Long userId) {
        ConversationContext context = conversationStore.getOrCreate(sessionId, userId);

        // 添加用户消息到历史
        context.addMessage(new ChatMessage("user", userMessage));

        // ① 取 Agent
        ReActAgent agent = agentFactory.get(sessionId).writer();

        // ② 内存策略
        memoryPolicy.beforeWriterCall(agent);

        // ③ 拼输入
        String userInput = buildInput(context);

        // ④ 调 Agent
        Msg resp = agent.call(Msg.builder()
                .role(MsgRole.USER)
                .textContent(userInput)
                .build()).block();

        String llmReply = resp.getTextContent();

        // 解析 LLM 回复
        String cleanReply = llmReply;
        boolean stageCompleted = false;

        // 检查是否包含阶段完成标记
        if (llmReply.contains(STAGE_COMPLETE_MARKER)) {
            stageCompleted = true;
            cleanReply = llmReply.replace(STAGE_COMPLETE_MARKER, "").trim();
        }

        // 提取结构化数据（如果有）
        extractStructuredData(llmReply, context);

        // 如果阶段完成，推进到下一阶段
        if (stageCompleted) {
            context.advanceStage();
            log.info("会话 {} 进入阶段: {}", sessionId, context.getCurrentStage());
        }

        // 保存 assistant 回复到历史
        context.addMessage(new ChatMessage("assistant", cleanReply));

        // 构建响应
        ResumeChatResponse.ResumeChatResponseBuilder responseBuilder = ResumeChatResponse.builder()
                .reply(cleanReply)
                .stage(context.getCurrentStage())
                .completed(context.isCompleted());

        if (context.isCompleted()) {
            responseBuilder.resumeContent(context.getPartialContent());
            // 完成后清理会话和 Agent 缓存
            conversationStore.remove(sessionId);
            agentFactory.remove(sessionId);
        }

        return responseBuilder.build();
    }

    /**
     * 构建发送给 Agent 的输入
     */
    private String buildInput(ConversationContext context) {
        StringBuilder sb = new StringBuilder();

        // 当前阶段信息
        sb.append("当前阶段：").append(context.getCurrentStage()).append("\n\n");

        // 如果已有部分内容，附加到输入中
        if (hasPartialContent(context.getPartialContent())) {
            String partialInfo = serializePartialContent(context.getPartialContent());
            sb.append("用户已提供的简历信息：\n").append(partialInfo).append("\n\n");
        }

        // 添加对话历史（最多保留最近 20 条）
        List<ChatMessage> history = context.getHistory();
        int startIndex = Math.max(0, history.size() - 20);
        if (startIndex < history.size()) {
            sb.append("对话历史：\n");
            for (int i = startIndex; i < history.size(); i++) {
                ChatMessage msg = history.get(i);
                String role = "user".equals(msg.getRole()) ? "用户" : "助手";
                sb.append(role).append("：").append(msg.getContent()).append("\n");
            }
        }

        // 当前用户消息（最后一条）
        List<ChatMessage> messages = context.getHistory();
        if (!messages.isEmpty()) {
            ChatMessage lastMsg = messages.get(messages.size() - 1);
            if ("user".equals(lastMsg.getRole())) {
                sb.append("\n当前用户消息：").append(lastMsg.getContent());
            }
        }

        return sb.toString();
    }

    /**
     * 从 LLM 回复中提取结构化数据
     */
    private void extractStructuredData(String reply, ConversationContext context) {
        int startIdx = reply.indexOf(DATA_START_MARKER);
        int endIdx = reply.indexOf(DATA_END_MARKER);

        if (startIdx == -1 || endIdx == -1 || endIdx <= startIdx) {
            return;
        }

        String jsonStr = reply.substring(startIdx + DATA_START_MARKER.length(), endIdx).trim();

        try {
            ResumeContent extracted = objectMapper.readValue(jsonStr, ResumeContent.class);
            mergeContent(context.getPartialContent(), extracted);
            log.debug("成功提取结构化数据: {}", jsonStr);
        } catch (JsonProcessingException e) {
            log.warn("解析结构化数据失败: {}", jsonStr, e);
        }
    }

    /**
     * 合并提取的内容到已有内容中
     */
    private void mergeContent(ResumeContent existing, ResumeContent extracted) {
        if (extracted.getBasicInfo() != null) {
            existing.setBasicInfo(extracted.getBasicInfo());
        }
        if (extracted.getEducation() != null && !extracted.getEducation().isEmpty()) {
            existing.getEducation().addAll(extracted.getEducation());
        }
        if (extracted.getWorkExperience() != null && !extracted.getWorkExperience().isEmpty()) {
            existing.getWorkExperience().addAll(extracted.getWorkExperience());
        }
        if (extracted.getProjectExperience() != null && !extracted.getProjectExperience().isEmpty()) {
            existing.getProjectExperience().addAll(extracted.getProjectExperience());
        }
        if (extracted.getSkills() != null && !extracted.getSkills().isEmpty()) {
            existing.setSkills(extracted.getSkills());
        }
        if (extracted.getSummary() != null) {
            existing.setSummary(extracted.getSummary());
        }
    }

    /**
     * 检查是否已有部分简历内容
     */
    private boolean hasPartialContent(ResumeContent content) {
        if (content == null) return false;
        return content.getBasicInfo() != null
                || (content.getEducation() != null && !content.getEducation().isEmpty())
                || (content.getWorkExperience() != null && !content.getWorkExperience().isEmpty())
                || (content.getProjectExperience() != null && !content.getProjectExperience().isEmpty())
                || (content.getSkills() != null && !content.getSkills().isEmpty())
                || content.getSummary() != null;
    }

    /**
     * 序列化已有部分内容用于提示 LLM
     */
    private String serializePartialContent(ResumeContent content) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(content);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
