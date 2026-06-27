package com.resume.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OptimizeTextRequest {

    @NotBlank(message = "简历文本内容不能为空")
    private String content;
}
