package com.resume.controller;

import com.resume.model.dto.ApiResponse;
import com.resume.model.dto.ApplyOptimizationRequest;
import com.resume.model.dto.OptimizationResult;
import com.resume.model.dto.OptimizeTextRequest;
import com.resume.model.entity.Resume;
import com.resume.service.CreditService;
import com.resume.service.OptimizeService;
import com.resume.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/optimize")
@RequiredArgsConstructor
public class OptimizeController {

    private final OptimizeService optimizeService;
    private final CreditService creditService;

    /**
     * 文本优化 - 粘贴纯文本进行优化
     */
    @PostMapping("/text")
    public ApiResponse<OptimizationResult> optimizeText(@Valid @RequestBody OptimizeTextRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        creditService.checkAndDeduct(userId, "resume_optimize");
        OptimizationResult result = optimizeService.optimizeText(userId, request.getContent());
        return ApiResponse.success(result);
    }

    /**
     * 文件上传优化 - 上传 PDF/Word 文件进行优化
     */
    @PostMapping("/file")
    public ApiResponse<OptimizationResult> optimizeFile(@RequestParam("file") MultipartFile file) {
        Long userId = SecurityUtils.getCurrentUserId();
        creditService.checkAndDeduct(userId, "resume_optimize");
        OptimizationResult result = optimizeService.optimizeFile(userId, file);
        return ApiResponse.success(result);
    }

    /**
     * 优化已有简历
     */
    @PostMapping("/resume/{id}")
    public ApiResponse<OptimizationResult> optimizeResume(@PathVariable("id") Long resumeId) {
        Long userId = SecurityUtils.getCurrentUserId();
        creditService.checkAndDeduct(userId, "resume_optimize");
        OptimizationResult result = optimizeService.optimizeResume(userId, resumeId);
        return ApiResponse.success(result);
    }

    /**
     * 应用优化建议 - 选择性采纳建议并更新简历
     */
    @PostMapping("/apply")
    public ApiResponse<Resume> applyOptimization(@Valid @RequestBody ApplyOptimizationRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Resume resume = optimizeService.applyOptimization(userId, request);
        return ApiResponse.success("优化建议已应用", resume);
    }
}
