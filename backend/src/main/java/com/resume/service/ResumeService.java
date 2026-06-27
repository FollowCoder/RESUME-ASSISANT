package com.resume.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.agent.AgentFactory;
import com.resume.agent.AgentMemoryPolicy;
import com.resume.model.dto.*;
import com.resume.model.entity.Resume;
import com.resume.repository.ResumeRepository;
import com.resume.util.BusinessException;
import com.resume.util.JsonUtils;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 简历服务 - 简历 CRUD 及 LLM 润色
 */
@Service
public class ResumeService {

    private static final Logger log = LoggerFactory.getLogger(ResumeService.class);

    private final ResumeRepository resumeRepository;
    private final AgentFactory agentFactory;
    private final AgentMemoryPolicy memoryPolicy;
    private final ObjectMapper objectMapper;

    public ResumeService(ResumeRepository resumeRepository,
                         AgentFactory agentFactory,
                         AgentMemoryPolicy memoryPolicy,
                         ObjectMapper objectMapper) {
        this.resumeRepository = resumeRepository;
        this.agentFactory = agentFactory;
        this.memoryPolicy = memoryPolicy;
        this.objectMapper = objectMapper;
    }

    /**
     * 创建简历
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public Resume createResume(Long userId, ResumeContent content, String title, String language, String templateId) {
        Map<String, Object> contentMap = objectMapper.convertValue(content, Map.class);

        Resume resume = Resume.builder()
                .userId(userId)
                .title(title != null ? title : generateTitle(content))
                .content(contentMap)
                .language(language != null ? language : "zh")
                .templateId(templateId)
                .version(1)
                .build();

        return resumeRepository.save(resume);
    }

    /**
     * 获取用户简历列表
     */
    public List<ResumeListResponse> getResumeList(Long userId) {
        List<Resume> resumes = resumeRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        return resumes.stream()
                .map(r -> ResumeListResponse.builder()
                        .id(r.getId())
                        .title(r.getTitle())
                        .language(r.getLanguage())
                        .templateId(r.getTemplateId())
                        .updatedAt(r.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 获取简历详情
     */
    public ResumeDetailResponse getResumeDetail(Long userId, Long resumeId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new BusinessException("简历不存在"));

        if (!resume.getUserId().equals(userId)) {
            throw new BusinessException("无权访问此简历");
        }

        ResumeContent content = objectMapper.convertValue(resume.getContent(), ResumeContent.class);

        return ResumeDetailResponse.builder()
                .id(resume.getId())
                .title(resume.getTitle())
                .content(content)
                .language(resume.getLanguage())
                .templateId(resume.getTemplateId())
                .filePath(resume.getFilePath())
                .version(resume.getVersion())
                .createdAt(resume.getCreatedAt())
                .updatedAt(resume.getUpdatedAt())
                .build();
    }

    /**
     * 更新简历
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public Resume updateResume(Long userId, Long resumeId, ResumeContent content) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new BusinessException("简历不存在"));

        if (!resume.getUserId().equals(userId)) {
            throw new BusinessException("无权修改此简历");
        }

        Map<String, Object> contentMap = objectMapper.convertValue(content, Map.class);
        resume.setContent(contentMap);
        resume.setVersion(resume.getVersion() + 1);

        return resumeRepository.save(resume);
    }

    /**
     * 表单式生成简历 - 接收表单数据，调用 LLM 润色各段描述
     */
    @Transactional
    public Resume generateFromForm(Long userId, ResumeFormRequest request) {
        ResumeContent content = request.getContent();

        // 调用 LLM 润色简历内容
        ResumeContent polishedContent = polishResumeContent(content);

        return createResume(userId, polishedContent, request.getTitle(), request.getLanguage(), request.getTemplateId());
    }

    /**
     * 使用 LLM 润色简历内容
     */
    private ResumeContent polishResumeContent(ResumeContent content) {
        try {
            String contentJson = objectMapper.writeValueAsString(content);

            // ① 取 Agent（使用 writer agent 进行润色）
            ReActAgent agent = agentFactory.get("resume-polish").writer();

            // ② 内存策略
            memoryPolicy.beforeWriterCall(agent);

            // ③ 拼输入
            String userInput = "请对以下简历内容进行润色和优化：\n" +
                    "1. 工作职责描述更加专业，使用动词开头\n" +
                    "2. 成果要量化，用数字说话\n" +
                    "3. 项目描述要突出技术难点和解决方案\n" +
                    "4. 个人总结要精炼有力\n" +
                    "5. 保持原有信息的真实性，不要编造\n\n" +
                    "请直接返回润色后的 JSON，格式与输入相同，不要添加任何额外说明文字。\n\n" +
                    "原始内容：\n" + contentJson;

            // ④ 调 Agent
            Msg resp = agent.call(Msg.builder()
                    .role(MsgRole.USER)
                    .textContent(userInput)
                    .build()).block();

            String polishedJson = resp.getTextContent();

            // ⑤ 解析（使用 JsonUtils 标准化处理）
            polishedJson = JsonUtils.cleanAndExtractJson(polishedJson);

            return objectMapper.readValue(polishedJson, ResumeContent.class);
        } catch (Exception e) {
            log.warn("简历润色失败，使用原始内容: {}", e.getMessage());
            return content;
        }
    }

    /**
     * 根据简历内容自动生成标题
     */
    private String generateTitle(ResumeContent content) {
        if (content.getBasicInfo() != null && content.getBasicInfo().getName() != null) {
            return content.getBasicInfo().getName() + "的简历";
        }
        return "我的简历";
    }
}
