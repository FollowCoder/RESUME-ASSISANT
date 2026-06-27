package com.resume.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchHistoryResponse {

    private Long id;
    private Long resumeId;
    private String resumeTitle;
    private String jdSnippet;
    private int matchScore;
    private String level;
    private LocalDateTime createdAt;
}
