package com.resume.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportRequest {

    @NotNull(message = "简历ID不能为空")
    private Long resumeId;

    @NotBlank(message = "导出格式不能为空")
    private String format;      // pdf / docx

    private String templateId;  // 可选，不传则使用简历已选模板

    private String language;    // zh / en，默认 zh
}
