package com.resume.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.agent.interviewer.InterviewContext;
import com.resume.agent.interviewer.InterviewContextStore;
import com.resume.model.dto.InterviewHistoryResponse;
import com.resume.model.dto.InterviewReport;
import com.resume.model.dto.InterviewStartRequest;
import com.resume.model.dto.InterviewStartResponse;
import com.resume.model.entity.Interview;
import com.resume.model.entity.Resume;
import com.resume.model.entity.UserProfile;
import com.resume.repository.InterviewRepository;
import com.resume.repository.ResumeRepository;
import com.resume.repository.UserProfileRepository;
import com.resume.util.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InterviewService {

    private static final Logger log = LoggerFactory.getLogger(InterviewService.class);

    private final InterviewRepository interviewRepository;
    private final ResumeRepository resumeRepository;
    private final UserProfileRepository userProfileRepository;
    private final InterviewAgentService interviewAgentService;
    private final InterviewContextStore contextStore;
    private final ObjectMapper objectMapper;

    public InterviewService(InterviewRepository interviewRepository,
                            ResumeRepository resumeRepository,
                            UserProfileRepository userProfileRepository,
                            InterviewAgentService interviewAgentService,
                            InterviewContextStore contextStore,
                            ObjectMapper objectMapper) {
        this.interviewRepository = interviewRepository;
        this.resumeRepository = resumeRepository;
        this.userProfileRepository = userProfileRepository;
        this.interviewAgentService = interviewAgentService;
        this.contextStore = contextStore;
        this.objectMapper = objectMapper;
    }

    /**
     * 开始面试
     */
    @Transactional
    public InterviewStartResponse startInterview(Long userId, InterviewStartRequest request) {
        // 验证简历属于用户
        Resume resume = resumeRepository.findById(request.getResumeId())
                .orElseThrow(() -> new BusinessException("简历不存在"));

        if (!resume.getUserId().equals(userId)) {
            throw new BusinessException("无权使用该简历");
        }

        // 创建面试记录
        Interview interview = Interview.builder()
                .userId(userId)
                .resumeId(request.getResumeId())
                .jdContent(request.getJdContent())
                .mode(request.getMode())
                .status("ongoing")
                .startedAt(LocalDateTime.now())
                .build();

        interview = interviewRepository.save(interview);

        // 获取用户画像
        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);

        // 构建简历内容字符串
        String resumeContent = buildResumeContentString(resume);

        // 初始化面试上下文
        InterviewContext context = new InterviewContext();
        context.setInterviewId(interview.getId());
        context.setUserId(userId);
        context.setResumeContent(resumeContent);
        context.setJdContent(request.getJdContent());
        context.setUserProfile(profile);

        contextStore.put(interview.getId(), context);

        // 构建 WebSocket URL
        String wsUrl = "/ws/interview/" + interview.getId();

        log.info("面试已创建: interviewId={}, userId={}", interview.getId(), userId);

        return InterviewStartResponse.builder()
                .interviewId(interview.getId())
                .wsUrl(wsUrl)
                .status("ongoing")
                .build();
    }

    /**
     * 结束面试并生成报告
     */
    @Transactional
    public InterviewReport endInterview(Long userId, Long interviewId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new BusinessException("面试记录不存在"));

        if (!interview.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该面试");
        }

        if ("completed".equals(interview.getStatus())) {
            // 已完成，直接返回报告
            return getReport(userId, interviewId);
        }

        // 获取上下文生成报告
        InterviewContext context = contextStore.get(interviewId);
        InterviewReport report;

        if (context != null && !context.getMessages().isEmpty()) {
            // 使用新的 AgentService 生成报告
            report = interviewAgentService.generateReport(String.valueOf(interviewId), context);
        } else {
            report = InterviewReport.builder()
                    .technicalDepth(0)
                    .communication(0)
                    .projectUnderstanding(0)
                    .adaptability(0)
                    .totalScore(0)
                    .passRate("低")
                    .strengths(List.of())
                    .weaknesses(List.of("面试对话记录不完整"))
                    .suggestions(List.of("建议重新进行面试"))
                    .overallComment("面试未正常完成，无法给出准确评估。")
                    .build();
        }

        // 更新面试状态
        interview.setStatus("completed");
        interview.setCompletedAt(LocalDateTime.now());
        interview.setTotalScore(report.getTotalScore());

        // 保存报告到数据库
        @SuppressWarnings("unchecked")
        Map<String, Object> reportMap = objectMapper.convertValue(report, Map.class);
        interview.setReport(reportMap);

        interviewRepository.save(interview);

        // 清理上下文和 AgentFactory 缓存
        contextStore.remove(interviewId);
        interviewAgentService.removeSession(String.valueOf(interviewId));

        log.info("面试已结束: interviewId={}, totalScore={}", interviewId, report.getTotalScore());

        return report;
    }

    /**
     * 获取面试报告
     */
    public InterviewReport getReport(Long userId, Long interviewId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new BusinessException("面试记录不存在"));

        if (!interview.getUserId().equals(userId)) {
            throw new BusinessException("无权查看该面试报告");
        }

        if (!"completed".equals(interview.getStatus())) {
            throw new BusinessException("面试尚未结束，无法查看报告");
        }

        Map<String, Object> reportMap = interview.getReport();
        if (reportMap == null || reportMap.isEmpty()) {
            throw new BusinessException("面试报告不存在");
        }

        return objectMapper.convertValue(reportMap, InterviewReport.class);
    }

    /**
     * 获取面试历史
     */
    public List<InterviewHistoryResponse> getHistory(Long userId) {
        List<Interview> interviews = interviewRepository.findByUserIdOrderByStartedAtDesc(userId);

        return interviews.stream().map(interview -> {
            String resumeTitle = resumeRepository.findById(interview.getResumeId())
                    .map(Resume::getTitle)
                    .orElse("未知简历");

            String jdSnippet = interview.getJdContent() != null
                    ? interview.getJdContent().substring(0, Math.min(100, interview.getJdContent().length()))
                    : "";

            return InterviewHistoryResponse.builder()
                    .id(interview.getId())
                    .resumeId(interview.getResumeId())
                    .resumeTitle(resumeTitle)
                    .jdSnippet(jdSnippet)
                    .mode(interview.getMode())
                    .status(interview.getStatus())
                    .totalScore(interview.getTotalScore())
                    .startedAt(interview.getStartedAt())
                    .completedAt(interview.getCompletedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    private String buildResumeContentString(Resume resume) {
        if (resume.getContent() == null) {
            return "简历内容为空";
        }
        try {
            return objectMapper.writeValueAsString(resume.getContent());
        } catch (Exception e) {
            return resume.getContent().toString();
        }
    }
}
