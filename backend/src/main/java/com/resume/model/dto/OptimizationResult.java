package com.resume.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptimizationResult {

    private int overallScore;
    private List<Suggestion> suggestions;
    private String optimizedContent;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Suggestion {
        private String id;
        private String dimension;
        private String original;
        private String optimized;
        private String reason;
        private String severity;
    }
}
