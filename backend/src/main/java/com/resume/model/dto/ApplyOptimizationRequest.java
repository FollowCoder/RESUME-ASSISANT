package com.resume.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ApplyOptimizationRequest {

    @NotNull(message = "简历ID不能为空")
    private Long resumeId;

    private List<String> acceptedSuggestionIds;
}
