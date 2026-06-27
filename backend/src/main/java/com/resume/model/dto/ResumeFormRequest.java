package com.resume.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 表单式简历提交请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeFormRequest {

    private String title;
    private String language;
    private String templateId;
    private ResumeContent content;
}
