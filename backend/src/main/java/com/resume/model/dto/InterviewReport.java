package com.resume.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InterviewReport {

    private int technicalDepth;       // 技术深度 0-100
    private int communication;        // 表达能力 0-100
    private int projectUnderstanding; // 项目理解 0-100
    private int adaptability;         // 应变能力 0-100
    private int totalScore;           // 综合评分 0-100
    private String passRate;          // 通过概率预估：高/中/低
    private List<String> strengths;   // 优势
    private List<String> weaknesses;  // 不足
    private List<String> suggestions; // 改进建议
    private String overallComment;    // 总体评语
}
