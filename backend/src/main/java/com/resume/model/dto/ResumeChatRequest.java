package com.resume.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对话式简历生成请求
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResumeChatRequest {
    private String message;
    private String sessionId;
}
