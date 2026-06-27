package com.resume.service;

import com.resume.model.dto.CreditResponse;
import com.resume.model.entity.CreditTransaction;
import com.resume.model.entity.User;
import com.resume.repository.CreditTransactionRepository;
import com.resume.repository.UserRepository;
import com.resume.util.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class CreditService {

    private static final Map<String, Integer> MODULE_COSTS = Map.of(
            "resume_write", 2,
            "resume_optimize", 1,
            "jd_match", 1,
            "interview", 3
    );

    private static final Map<String, String> MODULE_NAMES = Map.of(
            "resume_write", "简历编写",
            "resume_optimize", "简历优化",
            "jd_match", "JD匹配",
            "interview", "模拟面试"
    );

    private final UserRepository userRepository;
    private final CreditTransactionRepository creditTransactionRepository;

    public CreditService(UserRepository userRepository, CreditTransactionRepository creditTransactionRepository) {
        this.userRepository = userRepository;
        this.creditTransactionRepository = creditTransactionRepository;
    }

    /**
     * 获取用户余额
     */
    public int getBalance(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        return user.getRemainingCredits();
    }

    /**
     * 检查余额并扣费（原子操作）
     */
    @Transactional
    public boolean checkAndDeduct(Long userId, String module) {
        int cost = getModuleCost(module);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (user.getRemainingCredits() < cost) {
            throw new BusinessException("余额不足，当前剩余 " + user.getRemainingCredits()
                    + " 次，本次操作需要 " + cost + " 次", HttpStatus.PAYMENT_REQUIRED);
        }

        // 扣减余额
        user.setRemainingCredits(user.getRemainingCredits() - cost);
        userRepository.save(user);

        // 记录交易
        CreditTransaction transaction = CreditTransaction.builder()
                .userId(userId)
                .module(module)
                .creditsUsed(cost)
                .description(MODULE_NAMES.getOrDefault(module, module) + " - 消耗 " + cost + " 次")
                .build();
        creditTransactionRepository.save(transaction);

        return true;
    }

    /**
     * 获取使用记录
     */
    public List<CreditResponse.TransactionItem> getTransactions(Long userId) {
        List<CreditTransaction> transactions = creditTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return transactions.stream()
                .map(t -> CreditResponse.TransactionItem.builder()
                        .id(t.getId())
                        .module(t.getModule())
                        .moduleName(MODULE_NAMES.getOrDefault(t.getModule(), t.getModule()))
                        .creditsUsed(t.getCreditsUsed())
                        .description(t.getDescription())
                        .createdAt(t.getCreatedAt())
                        .build())
                .toList();
    }

    /**
     * 增加额度（购买/充值）
     */
    @Transactional
    public int addCredits(Long userId, int amount, String description) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        user.setRemainingCredits(user.getRemainingCredits() + amount);
        userRepository.save(user);

        // 记录充值交易
        CreditTransaction transaction = CreditTransaction.builder()
                .userId(userId)
                .module("purchase")
                .creditsUsed(-amount) // 负数表示充值
                .description(description)
                .build();
        creditTransactionRepository.save(transaction);

        return user.getRemainingCredits();
    }

    /**
     * 获取模块单价
     */
    public int getModuleCost(String module) {
        Integer cost = MODULE_COSTS.get(module);
        if (cost == null) {
            throw new BusinessException("未知的计费模块: " + module);
        }
        return cost;
    }
}
