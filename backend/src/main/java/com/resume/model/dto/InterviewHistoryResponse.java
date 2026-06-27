package com.resume.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InterviewHistoryResponse {

    private Long id;
    private Long resumeId;
    private String resumeTitle;
    private String jdSnippet;
    private String mode;
    private String status;
    private Integer totalScore;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
