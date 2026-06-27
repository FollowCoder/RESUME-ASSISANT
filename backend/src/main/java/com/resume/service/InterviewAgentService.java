package com.resume.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.agent.AgentFactory;
import com.resume.agent.AgentMemoryPolicy;
import com.resume.agent.interviewer.InterviewContext;
import com.resume.model.dto.InterviewMessage;
import com.resume.model.dto.InterviewReport;
import com.resume.util.JsonUtils;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 面试服务 - 使用 AgentScope 规范重构
 * 核心原则：所有 LLM 调用只经过 ReActAgent
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewAgentService {

    private final AgentFactory agentFactory;
    private final AgentMemoryPolicy memoryPolicy;
    private final ObjectMapper objectMapper;

    /**
     * 生成面试开场白
     */
    public String generateOpening(String sessionId, InterviewContext context) {
        // ① 取 Agent
        ReActAgent agent = agentFactory.get(sessionId).interviewer();

        // ② 内存策略
        memoryPolicy.beforeInterviewerCall(agent);

        // ③ 拼输入
        String userInput = buildOpeningInput(context);

        // ④ 调 Agent
        Msg resp = agent.call(Msg.builder()
                .role(MsgRole.USER)
                .textContent(userInput)
                .build()).block();

        return resp.getTextContent().trim();
    }

    /**
     * 处理候选人回答，生成下一个问题
     */
    public String processAnswer(String sessionId, String answer, InterviewContext context) {
        // ① 取 Agent
        ReActAgent agent = agentFactory.get(sessionId).interviewer();

        // ② 内存策略
        memoryPolicy.beforeInterviewerCall(agent);

        // ③ 拼输入
        String userInput = buildAnswerInput(answer, context);

        // ④ 调 Agent
        Msg resp = agent.call(Msg.builder()
                .role(MsgRole.USER)
                .textContent(userInput)
                .build()).block();

        return resp.getTextContent().trim();
    }

    /**
     * 生成面试评估报告
     */
    public InterviewReport generateReport(String sessionId, InterviewContext context) {
        // ① 取 Agent
        ReActAgent agent = agentFactory.get(sessionId).report();

        // ② 内存策略
        memoryPolicy.beforeReportCall(agent);

        // ③ 拼输入
        String userInput = buildReportInput(context);

        // ④ 调 Agent
        Msg resp = agent.call(Msg.builder()
                .role(MsgRole.USER)
                .textContent(userInput)
                .build()).block();

        // ⑤ 解析容错（必须 try-catch + 降级默认值）
        return parseReport(resp.getTextContent());
    }

    private String buildOpeningInput(InterviewContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("请开始面试，先做一个简短的自我介绍（作为面试官），然后邀请候选人做自我介绍。\n\n");

        appendContextInfo(sb, context);

        return sb.toString();
    }

    private String buildAnswerInput(String answer, InterviewContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("候选人回答：").append(answer).append("\n\n");

        appendContextInfo(sb, context);

        // 添加已问问题
        if (!context.getAskedQuestions().isEmpty()) {
            sb.append("## 已问问题\n");
            for (int i = 0; i < context.getAskedQuestions().size(); i++) {
                sb.append(i + 1).append(". ").append(context.getAskedQuestions().get(i)).append("\n");
            }
        }

        return sb.toString();
    }

    private void appendContextInfo(StringBuilder sb, InterviewContext context) {
        sb.append("## 候选人背景\n");
        if (context.getUserProfile() != null) {
            var profile = context.getUserProfile();
            if (profile.getWorkYears() != null) {
                sb.append("- 工作年限：").append(profile.getWorkYears()).append("\n");
            }
            if (profile.getTechDirection() != null && !profile.getTechDirection().isEmpty()) {
                sb.append("- 技术方向：").append(String.join("、", profile.getTechDirection())).append("\n");
            }
            if (profile.getTargetPosition() != null) {
                sb.append("- 目标职位：").append(profile.getTargetPosition()).append("\n");
            }
            if (profile.getCoreSkills() != null && !profile.getCoreSkills().isEmpty()) {
                sb.append("- 核心技能：").append(String.join("、", profile.getCoreSkills())).append("\n");
            }
        }

        sb.append("\n## 简历摘要\n");
        sb.append(truncate(context.getResumeContent(), 1500)).append("\n\n");

        sb.append("## 目标岗位JD关键要求\n");
        sb.append(truncate(context.getJdContent(), 1000)).append("\n\n");
    }

    private String buildReportInput(InterviewContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 面试对话记录\n\n");

        for (InterviewMessage msg : context.getMessages()) {
            String role = switch (msg.getType()) {
                case "interviewer" -> "面试官";
                case "candidate" -> "候选人";
                default -> "系统";
            };
            sb.append("**").append(role).append("**：").append(msg.getContent()).append("\n\n");
        }

        sb.append("\n## 目标岗位\n");
        sb.append(truncate(context.getJdContent(), 500)).append("\n");

        return sb.toString();
    }

    /**
     * 解析面试报告 - 遵循 AgentScope 规范：必须 try-catch + 降级默认值
     */
    private InterviewReport parseReport(String response) {
        try {
            String json = JsonUtils.cleanAndExtractJson(response);
            JsonNode root = objectMapper.readTree(json);

            return InterviewReport.builder()
                    .technicalDepth(root.path("technicalDepth").asInt(60))
                    .communication(root.path("communication").asInt(60))
                    .projectUnderstanding(root.path("projectUnderstanding").asInt(60))
                    .adaptability(root.path("adaptability").asInt(60))
                    .totalScore(root.path("totalScore").asInt(60))
                    .passRate(root.path("passRate").asText("中"))
                    .strengths(parseStringList(root.path("strengths")))
                    .weaknesses(parseStringList(root.path("weaknesses")))
                    .suggestions(parseStringList(root.path("suggestions")))
                    .overallComment(root.path("overallComment").asText(""))
                    .build();
        } catch (Exception e) {
            log.error("解析面试报告失败: {}", e.getMessage(), e);
            // 降级默认值
            return InterviewReport.builder()
                    .technicalDepth(60)
                    .communication(60)
                    .projectUnderstanding(60)
                    .adaptability(60)
                    .totalScore(60)
                    .passRate("中")
                    .strengths(List.of("面试已完成"))
                    .weaknesses(List.of("评估报告生成异常，请参考对话内容"))
                    .suggestions(List.of("建议重新进行面试以获取准确评估"))
                    .overallComment("由于系统原因，评估报告生成不完整。")
                    .build();
        }
    }

    private List<String> parseStringList(JsonNode node) {
        List<String> result = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode item : node) {
                result.add(item.asText());
            }
        }
        return result;
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    /**
     * 移除会话缓存
     */
    public void removeSession(String sessionId) {
        agentFactory.remove(sessionId);
    }
}
