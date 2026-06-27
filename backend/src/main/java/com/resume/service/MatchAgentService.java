package com.resume.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.agent.AgentFactory;
import com.resume.agent.AgentMemoryPolicy;
import com.resume.model.dto.MatchAnalysisResult;
import com.resume.model.entity.UserProfile;
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
 * 匹配分析服务 - 使用 AgentScope 规范重构
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchAgentService {

    private final AgentFactory agentFactory;
    private final AgentMemoryPolicy memoryPolicy;
    private final ObjectMapper objectMapper;

    /**
     * 分析简历与JD的匹配度
     */
    public MatchAnalysisResult analyze(String sessionId, String resumeContent, String jdContent, UserProfile profile) {
        // ① 取 Agent
        ReActAgent agent = agentFactory.get(sessionId).matcher();

        // ② 内存策略
        memoryPolicy.beforeMatchCall(agent);

        // ③ 拼输入
        String userInput = buildUserMessage(resumeContent, jdContent, profile);

        // ④ 调 Agent
        Msg resp = agent.call(Msg.builder()
                .role(MsgRole.USER)
                .textContent(userInput)
                .build()).block();

        // ⑤ 解析容错
        return parseResult(resp.getTextContent());
    }

    private String buildUserMessage(String resumeContent, String jdContent, UserProfile profile) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 简历内容\n");
        sb.append(resumeContent).append("\n\n");
        sb.append("## 职位描述(JD)\n");
        sb.append(jdContent).append("\n\n");

        if (profile != null) {
            sb.append("## 求职者补充信息\n");
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

        return sb.toString();
    }

    /**
     * 解析匹配结果 - 遵循 AgentScope 规范：必须 try-catch + 降级默认值
     */
    private MatchAnalysisResult parseResult(String response) {
        try {
            String json = JsonUtils.cleanAndExtractJson(response);
            JsonNode root = objectMapper.readTree(json);

            int totalScore = root.path("totalScore").asInt(0);
            String level = root.path("level").asText(determineLevel(totalScore));

            // 解析维度分数
            JsonNode dimNode = root.path("dimensions");
            MatchAnalysisResult.DimensionScores dimensions = MatchAnalysisResult.DimensionScores.builder()
                    .skillMatch(dimNode.path("skillMatch").asInt(0))
                    .experienceMatch(dimNode.path("experienceMatch").asInt(0))
                    .educationMatch(dimNode.path("educationMatch").asInt(0))
                    .keywordCoverage(dimNode.path("keywordCoverage").asInt(0))
                    .build();

            // 解析差距项
            List<MatchAnalysisResult.GapItem> gaps = new ArrayList<>();
            JsonNode gapsNode = root.path("gaps");
            if (gapsNode.isArray()) {
                for (JsonNode gapNode : gapsNode) {
                    gaps.add(MatchAnalysisResult.GapItem.builder()
                            .category(gapNode.path("category").asText(""))
                            .requirement(gapNode.path("requirement").asText(""))
                            .suggestion(gapNode.path("suggestion").asText(""))
                            .build());
                }
            }

            // 解析改进建议
            List<String> improvements = new ArrayList<>();
            JsonNode improvementsNode = root.path("improvements");
            if (improvementsNode.isArray()) {
                for (JsonNode item : improvementsNode) {
                    improvements.add(item.asText());
                }
            }

            return MatchAnalysisResult.builder()
                    .totalScore(totalScore)
                    .level(level.isEmpty() ? determineLevel(totalScore) : level)
                    .dimensions(dimensions)
                    .gaps(gaps)
                    .improvements(improvements)
                    .build();

        } catch (Exception e) {
            log.error("解析 LLM 匹配分析结果失败: {}", e.getMessage(), e);
            // 降级默认值
            return MatchAnalysisResult.builder()
                    .totalScore(0)
                    .level("较差")
                    .dimensions(MatchAnalysisResult.DimensionScores.builder()
                            .skillMatch(0)
                            .experienceMatch(0)
                            .educationMatch(0)
                            .keywordCoverage(0)
                            .build())
                    .gaps(List.of())
                    .improvements(List.of("分析结果解析失败，请重试"))
                    .build();
        }
    }

    private String determineLevel(int score) {
        if (score >= 85) return "极佳";
        if (score >= 70) return "良好";
        if (score >= 55) return "一般";
        return "较差";
    }
}
