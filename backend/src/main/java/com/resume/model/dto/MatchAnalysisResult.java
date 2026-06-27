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
public class MatchAnalysisResult {

    private int totalScore;
    private String level;
    private DimensionScores dimensions;
    private List<GapItem> gaps;
    private List<String> improvements;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DimensionScores {
        private int skillMatch;
        private int experienceMatch;
        private int educationMatch;
        private int keywordCoverage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GapItem {
        private String category;
        private String requirement;
        private String suggestion;
    }
}
