package com.resume.controller;

import com.resume.model.dto.ApiResponse;
import com.resume.model.dto.MatchAnalysisResult;
import com.resume.model.dto.MatchAnalyzeRequest;
import com.resume.model.dto.MatchHistoryResponse;
import com.resume.service.CreditService;
import com.resume.service.MatchService;
import com.resume.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/match")
public class MatchController {

    private final MatchService matchService;
    private final CreditService creditService;

    public MatchController(MatchService matchService, CreditService creditService) {
        this.matchService = matchService;
        this.creditService = creditService;
    }

    /**
     * 执行JD匹配分析
     */
    @PostMapping("/analyze")
    public ApiResponse<MatchAnalysisResult> analyze(@Valid @RequestBody MatchAnalyzeRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        creditService.checkAndDeduct(userId, "jd_match");
        MatchAnalysisResult result = matchService.analyze(userId, request);
        return ApiResponse.success(result);
    }

    /**
     * 获取匹配历史列表
     */
    @GetMapping("/history")
    public ApiResponse<List<MatchHistoryResponse>> getHistory() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<MatchHistoryResponse> history = matchService.getHistory(userId);
        return ApiResponse.success(history);
    }

    /**
     * 获取匹配详情
     */
    @GetMapping("/{id}")
    public ApiResponse<MatchAnalysisResult> getDetail(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        MatchAnalysisResult result = matchService.getDetail(userId, id);
        return ApiResponse.success(result);
    }
}
