package com.resume.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditResponse {

    private int balance;
    private List<TransactionItem> transactions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TransactionItem {
        private Long id;
        private String module;
        private String moduleName;
        private int creditsUsed;
        private String description;
        private LocalDateTime createdAt;
    }
}
