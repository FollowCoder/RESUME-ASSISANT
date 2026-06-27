package com.resume.controller;

import com.resume.model.dto.*;
import com.resume.model.entity.Resume;
import com.resume.service.CreditService;
import com.resume.service.ExportService;
import com.resume.service.ResumeAgentService;
import com.resume.service.ResumeService;
import com.resume.service.TemplateService;
import com.resume.util.SecurityUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 简历控制器 - 提供简历编写相关 API
 */
@RestController
@RequestMapping("/api/resume")
public class ResumeController {

    private final ResumeService resumeService;
    private final ResumeAgentService resumeAgentService;
    private final TemplateService templateService;
    private final ExportService exportService;
    private final CreditService creditService;

    public ResumeController(ResumeService resumeService, ResumeAgentService resumeAgentService,
                            TemplateService templateService, ExportService exportService,
                            CreditService creditService) {
        this.resumeService = resumeService;
        this.resumeAgentService = resumeAgentService;
        this.templateService = templateService;
        this.exportService = exportService;
        this.creditService = creditService;
    }

    /**
     * 对话式生成简历
     * POST /api/resume/chat
     */
    @PostMapping("/chat")
    public ApiResponse<ResumeChatResponse> chat(@RequestBody ResumeChatRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = "session_" + userId + "_" + System.currentTimeMillis();
        }

        // 使用新的 AgentService
        ResumeChatResponse response = resumeAgentService.chat(sessionId, request.getMessage(), userId);

        // 如果对话完成，自动保存简历并扣费
        if (response.isCompleted() && response.getResumeContent() != null) {
            creditService.checkAndDeduct(userId, "resume_write");
            Resume saved = resumeService.createResume(userId, response.getResumeContent(), null, "zh", null);
            response.setResumeId(saved.getId());
        }

        return ApiResponse.success(response);
    }

    /**
     * 表单式提交生成简历
     * POST /api/resume/form
     */
    @PostMapping("/form")
    public ApiResponse<ResumeDetailResponse> generateFromForm(@RequestBody ResumeFormRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        creditService.checkAndDeduct(userId, "resume_write");
        Resume resume = resumeService.generateFromForm(userId, request);
        ResumeDetailResponse detail = resumeService.getResumeDetail(userId, resume.getId());
        return ApiResponse.success("简历生成成功", detail);
    }

    /**
     * 获取用户简历列表
     * GET /api/resume/list
     */
    @GetMapping("/list")
    public ApiResponse<List<ResumeListResponse>> getResumeList() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<ResumeListResponse> list = resumeService.getResumeList(userId);
        return ApiResponse.success(list);
    }

    /**
     * 获取简历详情
     * GET /api/resume/{id}
     */
    @GetMapping("/{id}")
    public ApiResponse<ResumeDetailResponse> getResumeDetail(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        ResumeDetailResponse detail = resumeService.getResumeDetail(userId, id);
        return ApiResponse.success(detail);
    }

    /**
     * 更新简历
     * PUT /api/resume/{id}
     */
    @PutMapping("/{id}")
    public ApiResponse<ResumeDetailResponse> updateResume(@PathVariable Long id, @RequestBody ResumeContent content) {
        Long userId = SecurityUtils.getCurrentUserId();
        resumeService.updateResume(userId, id, content);
        ResumeDetailResponse detail = resumeService.getResumeDetail(userId, id);
        return ApiResponse.success("简历更新成功", detail);
    }

    /**
     * 获取模板列表
     * GET /api/resume/templates
     */
    @GetMapping("/templates")
    public ApiResponse<List<TemplateService.TemplateInfo>> getTemplates() {
        return ApiResponse.success(templateService.getTemplateList());
    }

    /**
     * 导出简历
     * POST /api/resume/export
     */
    @PostMapping("/export")
    public ResponseEntity<byte[]> exportResume(@RequestBody ExportRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        // 获取简历详情
        ResumeDetailResponse detail = resumeService.getResumeDetail(userId, request.getResumeId());
        ResumeContent content = detail.getContent();
        if (content == null) {
            return ResponseEntity.badRequest().body("简历内容为空，无法导出".getBytes());
        }

        // 确定模板ID
        String templateId = request.getTemplateId();
        if (templateId == null || templateId.isBlank()) {
            templateId = detail.getTemplateId() != null ? detail.getTemplateId() : "classic";
        }

        // 确定语言
        String language = request.getLanguage();
        if (language == null || language.isBlank()) {
            language = detail.getLanguage() != null ? detail.getLanguage() : "zh";
        }

        String format = request.getFormat().toLowerCase();
        byte[] fileData;
        String contentType;
        String fileExtension;

        if ("pdf".equals(format)) {
            fileData = exportService.exportPdf(content, templateId, language);
            contentType = "application/pdf";
            fileExtension = "pdf";
        } else if ("docx".equals(format)) {
            fileData = exportService.exportWord(content, templateId, language);
            contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            fileExtension = "docx";
        } else {
            return ResponseEntity.badRequest().build();
        }

        // 保存文件
        exportService.saveExportFile(fileData, fileExtension, request.getResumeId());

        // 构建文件名
        String filename = "resume_" + request.getResumeId() + "." + fileExtension;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(fileData.length)
                .body(fileData);
    }
}
