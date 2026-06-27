package com.resume.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 简历列表响应项
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeListResponse {
    private Long id;
    private String title;
    private String language;
    private String templateId;
    private LocalDateTime updatedAt;
}
