package com.resume.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.model.dto.MatchAnalysisResult;
import com.resume.model.dto.MatchAnalyzeRequest;
import com.resume.model.dto.MatchHistoryResponse;
import com.resume.model.entity.JdMatch;
import com.resume.model.entity.Resume;
import com.resume.model.entity.UserProfile;
import com.resume.repository.JdMatchRepository;
import com.resume.repository.ResumeRepository;
import com.resume.repository.UserProfileRepository;
import com.resume.util.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MatchService {

    private static final Logger log = LoggerFactory.getLogger(MatchService.class);

    private final MatchAgentService matchAgentService;
    private final JdMatchRepository jdMatchRepository;
    private final ResumeRepository resumeRepository;
    private final UserProfileRepository userProfileRepository;
    private final ObjectMapper objectMapper;

    public MatchService(MatchAgentService matchAgentService,
                        JdMatchRepository jdMatchRepository,
                        ResumeRepository resumeRepository,
                        UserProfileRepository userProfileRepository,
                        ObjectMapper objectMapper) {
        this.matchAgentService = matchAgentService;
        this.jdMatchRepository = jdMatchRepository;
        this.resumeRepository = resumeRepository;
        this.userProfileRepository = userProfileRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 执行JD匹配分析
     */
    public MatchAnalysisResult analyze(Long userId, MatchAnalyzeRequest request) {
        // 获取简历并验证属于当前用户
        Resume resume = resumeRepository.findById(request.getResumeId())
                .orElseThrow(() -> new BusinessException("简历不存在", HttpStatus.NOT_FOUND));

        if (!resume.getUserId().equals(userId)) {
            throw new BusinessException("无权访问该简历", HttpStatus.FORBIDDEN);
        }

        // 获取用户画像（可选）
        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);

        // 将简历内容转为文本
        String resumeContent = convertResumeToText(resume);

        // 调用 Agent 分析（使用新的 AgentService）
        String sessionId = "match_" + userId + "_" + UUID.randomUUID();
        MatchAnalysisResult result = matchAgentService.analyze(sessionId, resumeContent, request.getJdContent(), profile);

        // 保存匹配记录
        saveMatchRecord(userId, request, result);

        return result;
    }

    /**
     * 获取匹配历史列表
     */
    public List<MatchHistoryResponse> getHistory(Long userId) {
        List<JdMatch> matches = jdMatchRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return matches.stream().map(match -> {
            // 获取关联的简历标题
            String resumeTitle = resumeRepository.findById(match.getResumeId())
                    .map(Resume::getTitle)
                    .orElse("已删除的简历");

            // JD 前50字摘要
            String jdSnippet = match.getJdContent() != null && match.getJdContent().length() > 50
                    ? match.getJdContent().substring(0, 50) + "..."
                    : match.getJdContent();

            // 从 analysis 中获取 level
            String level = "未知";
            if (match.getAnalysis() != null && match.getAnalysis().containsKey("level")) {
                level = String.valueOf(match.getAnalysis().get("level"));
            }

            return MatchHistoryResponse.builder()
                    .id(match.getId())
                    .resumeId(match.getResumeId())
                    .resumeTitle(resumeTitle)
                    .jdSnippet(jdSnippet)
                    .matchScore(match.getMatchScore() != null ? match.getMatchScore() : 0)
                    .level(level)
                    .createdAt(match.getCreatedAt())
                    .build();
        }).toList();
    }

    /**
     * 获取匹配详情
     */
    public MatchAnalysisResult getDetail(Long userId, Long matchId) {
        JdMatch match = jdMatchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException("匹配记录不存在", HttpStatus.NOT_FOUND));

        if (!match.getUserId().equals(userId)) {
            throw new BusinessException("无权访问该记录", HttpStatus.FORBIDDEN);
        }

        // 从 analysis JSONB 字段反序列化
        if (match.getAnalysis() == null) {
            throw new BusinessException("匹配分析数据不存在");
        }

        try {
            String json = objectMapper.writeValueAsString(match.getAnalysis());
            return objectMapper.readValue(json, MatchAnalysisResult.class);
        } catch (Exception e) {
            log.error("反序列化匹配分析结果失败: {}", e.getMessage(), e);
            throw new BusinessException("匹配分析数据解析失败");
        }
    }

    private String convertResumeToText(Resume resume) {
        if (resume.getContent() == null) {
            return "（简历内容为空）";
        }
        try {
            return objectMapper.writeValueAsString(resume.getContent());
        } catch (Exception e) {
            log.warn("简历内容序列化失败，使用 toString: {}", e.getMessage());
            return resume.getContent().toString();
        }
    }

    private void saveMatchRecord(Long userId, MatchAnalyzeRequest request, MatchAnalysisResult result) {
        try {
            // 将 MatchAnalysisResult 转为 Map 存储到 JSONB
            String resultJson = objectMapper.writeValueAsString(result);
            Map<String, Object> analysisMap = objectMapper.readValue(resultJson,
                    new TypeReference<Map<String, Object>>() {});

            JdMatch match = JdMatch.builder()
                    .userId(userId)
                    .resumeId(request.getResumeId())
                    .jdContent(request.getJdContent())
                    .matchScore(result.getTotalScore())
                    .analysis(analysisMap)
                    .build();

            jdMatchRepository.save(match);
        } catch (Exception e) {
            log.error("保存匹配记录失败: {}", e.getMessage(), e);
            // 不抛异常，分析结果仍然返回给用户
        }
    }
}
