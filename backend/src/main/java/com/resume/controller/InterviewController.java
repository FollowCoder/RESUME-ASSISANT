package com.resume.controller;

import com.resume.model.dto.*;
import com.resume.service.CreditService;
import com.resume.service.InterviewService;
import com.resume.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interview")
public class InterviewController {

    private final InterviewService interviewService;
    private final CreditService creditService;

    public InterviewController(InterviewService interviewService, CreditService creditService) {
        this.interviewService = interviewService;
        this.creditService = creditService;
    }

    /**
     * 开始面试
     */
    @PostMapping("/start")
    public ApiResponse<InterviewStartResponse> startInterview(@Valid @RequestBody InterviewStartRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        creditService.checkAndDeduct(userId, "interview");
        InterviewStartResponse response = interviewService.startInterview(userId, request);
        return ApiResponse.success("面试已创建", response);
    }

    /**
     * 结束面试
     */
    @PostMapping("/{id}/end")
    public ApiResponse<InterviewReport> endInterview(@PathVariable("id") Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        InterviewReport report = interviewService.endInterview(userId, id);
        return ApiResponse.success("面试评估报告已生成", report);
    }

    /**
     * 获取面试报告
     */
    @GetMapping("/{id}/report")
    public ApiResponse<InterviewReport> getReport(@PathVariable("id") Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        InterviewReport report = interviewService.getReport(userId, id);
        return ApiResponse.success(report);
    }

    /**
     * 获取面试历史
     */
    @GetMapping("/history")
    public ApiResponse<List<InterviewHistoryResponse>> getHistory() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<InterviewHistoryResponse> history = interviewService.getHistory(userId);
        return ApiResponse.success(history);
    }
}
