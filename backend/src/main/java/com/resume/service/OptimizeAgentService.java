package com.resume.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.agent.AgentFactory;
import com.resume.agent.AgentMemoryPolicy;
import com.resume.model.dto.OptimizationResult;
import com.resume.model.entity.UserProfile;
import com.resume.util.BusinessException;
import com.resume.util.JsonUtils;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 简历优化服务 - 使用 AgentScope 规范重构
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OptimizeAgentService {

    private final AgentFactory agentFactory;
    private final AgentMemoryPolicy memoryPolicy;
    private final ObjectMapper objectMapper;

    /**
     * 对简历文本进行优化分析
     */
    public OptimizationResult optimize(String sessionId, String resumeText, UserProfile profile) {
        // ① 取 Agent
        ReActAgent agent = agentFactory.get(sessionId).optimizer();

        // ② 内存策略
        memoryPolicy.beforeOptimizeCall(agent);

        // ③ 拼输入
        String userInput = buildUserInput(resumeText, profile);

        // ④ 调 Agent
        Msg resp = agent.call(Msg.builder()
                .role(MsgRole.USER)
                .textContent(userInput)
                .build()).block();

        // ⑤ 解析容错
        return parseOptimizationResult(resp.getTextContent());
    }

    private String buildUserInput(String resumeText, UserProfile profile) {
        StringBuilder sb = new StringBuilder();

        // 用户背景
        if (profile != null) {
            sb.append("用户背景：");
            if (profile.getWorkYears() != null) {
                sb.append("工作年限[").append(profile.getWorkYears()).append("]，");
            }
            if (profile.getTargetPosition() != null) {
                sb.append("目标岗位[").append(profile.getTargetPosition()).append("]，");
            }
            if (profile.getTechDirection() != null && !profile.getTechDirection().isEmpty()) {
                sb.append("技术方向[").append(String.join("/", profile.getTechDirection())).append("]，");
            }
            if (profile.getCoreSkills() != null && !profile.getCoreSkills().isEmpty()) {
                sb.append("核心技能[").append(String.join("/", profile.getCoreSkills())).append("]，");
            }
            if (profile.getTargetIndustry() != null && !profile.getTargetIndustry().isEmpty()) {
                sb.append("目标行业[").append(String.join("/", profile.getTargetIndustry())).append("]");
            }
        } else {
            sb.append("用户背景：未提供详细信息");
        }

        sb.append("\n\n请分析并优化以下简历：\n\n").append(resumeText);

        return sb.toString();
    }

    /**
     * 解析优化结果 - 遵循 AgentScope 规范：必须 try-catch + 降级默认值
     */
    private OptimizationResult parseOptimizationResult(String response) {
        try {
            String json = JsonUtils.cleanAndExtractJson(response);
            JsonNode root = objectMapper.readTree(json);

            int overallScore = root.path("overallScore").asInt(60);
            String optimizedContent = root.path("optimizedContent").asText("");

            List<OptimizationResult.Suggestion> suggestions = new ArrayList<>();
            JsonNode suggestionsNode = root.path("suggestions");
            if (suggestionsNode.isArray()) {
                for (JsonNode node : suggestionsNode) {
                    OptimizationResult.Suggestion suggestion = OptimizationResult.Suggestion.builder()
                            .id(UUID.randomUUID().toString())
                            .dimension(node.path("dimension").asText("structure"))
                            .original(node.path("original").asText(""))
                            .optimized(node.path("optimized").asText(""))
                            .reason(node.path("reason").asText(""))
                            .severity(node.path("severity").asText("medium"))
                            .build();
                    suggestions.add(suggestion);
                }
            }

            return OptimizationResult.builder()
                    .overallScore(overallScore)
                    .suggestions(suggestions)
                    .optimizedContent(optimizedContent)
                    .build();

        } catch (Exception e) {
            log.error("解析LLM优化结果失败: {}", response, e);
            throw new BusinessException("AI分析结果解析失败，请重试");
        }
    }
}
