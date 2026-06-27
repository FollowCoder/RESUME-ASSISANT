package com.resume.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.model.dto.ApplyOptimizationRequest;
import com.resume.model.dto.OptimizationResult;
import com.resume.model.entity.Resume;
import com.resume.model.entity.UserProfile;
import com.resume.repository.ResumeRepository;
import com.resume.repository.UserProfileRepository;
import com.resume.util.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class OptimizeService {

    private final OptimizeAgentService optimizeAgentService;
    private final FileParserService fileParserService;
    private final ResumeRepository resumeRepository;
    private final UserProfileRepository userProfileRepository;
    private final ObjectMapper objectMapper;

    /**
     * 优化纯文本简历
     */
    public OptimizationResult optimizeText(Long userId, String text) {
        if (text == null || text.isBlank()) {
            throw new BusinessException("简历文本内容不能为空");
        }

        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);
        String sessionId = "optimize_" + userId + "_" + UUID.randomUUID();
        return optimizeAgentService.optimize(sessionId, text, profile);
    }

    /**
     * 优化上传的文件简历
     */
    public OptimizationResult optimizeFile(Long userId, MultipartFile file) {
        String text = fileParserService.parseFile(file);
        return optimizeText(userId, text);
    }

    /**
     * 优化已有的简历
     */
    public OptimizationResult optimizeResume(Long userId, Long resumeId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new BusinessException("简历不存在", HttpStatus.NOT_FOUND));

        if (!resume.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此简历", HttpStatus.FORBIDDEN);
        }

        String resumeText = extractResumeText(resume);
        return optimizeText(userId, resumeText);
    }

    /**
     * 应用优化建议到简历
     */
    public Resume applyOptimization(Long userId, ApplyOptimizationRequest request) {
        Resume resume = resumeRepository.findById(request.getResumeId())
                .orElseThrow(() -> new BusinessException("简历不存在", HttpStatus.NOT_FOUND));

        if (!resume.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此简历", HttpStatus.FORBIDDEN);
        }

        List<String> acceptedIds = request.getAcceptedSuggestionIds();
        if (acceptedIds == null || acceptedIds.isEmpty()) {
            throw new BusinessException("请至少选择一条优化建议");
        }

        // 更新简历版本
        resume.setVersion(resume.getVersion() + 1);

        // 将接受的建议ID保存到简历的content中以便追踪
        Map<String, Object> content = resume.getContent();
        if (content == null) {
            content = new HashMap<>();
        }
        content.put("lastOptimization", Map.of(
                "acceptedSuggestionIds", acceptedIds,
                "version", resume.getVersion()
        ));
        resume.setContent(content);

        return resumeRepository.save(resume);
    }

    /**
     * 从 Resume entity 中提取文本内容
     */
    private String extractResumeText(Resume resume) {
        Map<String, Object> content = resume.getContent();
        if (content == null || content.isEmpty()) {
            throw new BusinessException("简历内容为空，无法进行优化");
        }

        try {
            // 尝试将 JSON 内容转换为可读文本
            StringBuilder textBuilder = new StringBuilder();

            if (content.containsKey("summary")) {
                textBuilder.append("个人总结：\n").append(content.get("summary")).append("\n\n");
            }
            if (content.containsKey("workExperience")) {
                textBuilder.append("工作经历：\n").append(objectMapper.writeValueAsString(content.get("workExperience"))).append("\n\n");
            }
            if (content.containsKey("projectExperience")) {
                textBuilder.append("项目经验：\n").append(objectMapper.writeValueAsString(content.get("projectExperience"))).append("\n\n");
            }
            if (content.containsKey("skills")) {
                textBuilder.append("技能清单：\n").append(objectMapper.writeValueAsString(content.get("skills"))).append("\n\n");
            }
            if (content.containsKey("education")) {
                textBuilder.append("教育背景：\n").append(objectMapper.writeValueAsString(content.get("education"))).append("\n\n");
            }

            String text = textBuilder.toString().trim();
            if (text.isEmpty()) {
                // 如果没有结构化字段，直接序列化整个content
                return objectMapper.writeValueAsString(content);
            }
            return text;
        } catch (Exception e) {
            log.error("提取简历文本失败", e);
            throw new BusinessException("简历内容格式异常");
        }
    }
}
