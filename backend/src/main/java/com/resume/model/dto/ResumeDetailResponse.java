package com.resume.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 简历详情响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeDetailResponse {
    private Long id;
    private String title;
    private ResumeContent content;
    private String language;
    private String templateId;
    private String filePath;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
