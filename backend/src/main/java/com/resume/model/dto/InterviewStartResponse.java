package com.resume.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InterviewStartResponse {

    private Long interviewId;
    private String wsUrl;    // WebSocket 连接地址
    private String status;
}
