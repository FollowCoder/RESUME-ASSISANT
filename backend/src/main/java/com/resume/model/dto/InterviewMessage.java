package com.resume.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InterviewMessage {

    private String type;     // interviewer/candidate/system
    private String content;  // 消息内容
    private String timestamp;
    private Map<String, Object> metadata; // 额外信息（如当前阶段）
}
