package com.resume.controller;

import com.resume.model.dto.ApiResponse;
import com.resume.model.dto.CreditResponse;
import com.resume.model.dto.PurchaseRequest;
import com.resume.service.CreditService;
import com.resume.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/credits")
public class CreditController {

    private final CreditService creditService;

    public CreditController(CreditService creditService) {
        this.creditService = creditService;
    }

    /**
     * 查询余额
     */
    @GetMapping("/balance")
    public ApiResponse<Map<String, Integer>> getBalance() {
        Long userId = SecurityUtils.getCurrentUserId();
        int balance = creditService.getBalance(userId);
        return ApiResponse.success(Map.of("balance", balance));
    }

    /**
     * 购买次数（模拟，直接加额度）
     */
    @PostMapping("/purchase")
    public ApiResponse<Map<String, Integer>> purchase(@Valid @RequestBody PurchaseRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        int newBalance = creditService.addCredits(userId, request.getAmount(), "充值 " + request.getAmount() + " 次");
        return ApiResponse.success(Map.of("balance", newBalance));
    }

    /**
     * 使用记录
     */
    @GetMapping("/transactions")
    public ApiResponse<List<CreditResponse.TransactionItem>> getTransactions() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<CreditResponse.TransactionItem> transactions = creditService.getTransactions(userId);
        return ApiResponse.success(transactions);
    }
}
