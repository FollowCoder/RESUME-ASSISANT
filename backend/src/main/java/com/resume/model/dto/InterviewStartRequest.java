package com.resume.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InterviewStartRequest {

    @NotNull(message = "简历ID不能为空")
    private Long resumeId;

    @NotBlank(message = "JD内容不能为空")
    private String jdContent;

    private String mode = "text";
}
