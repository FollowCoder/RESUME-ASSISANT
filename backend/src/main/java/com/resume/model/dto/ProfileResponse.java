package com.resume.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ProfileResponse {
    private Long id;
    private String workYears;
    private List<String> techDirection;
    private String targetPosition;
    private List<String> targetIndustry;
    private String salaryRange;
    private List<Map<String, Object>> education;
    private List<String> coreSkills;
    private LocalDateTime updatedAt;
    private boolean profileCompleted;  // 画像是否已完善
}
