package com.resume.agent.interviewer;

import com.resume.config.PromptLoader;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.Model;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 面试报告 Agent Builder - 使用 main 模型（生成评估报告）
 */
@Component
@RequiredArgsConstructor
public class InterviewReportAgentBuilder {

    @Qualifier("mainChatModel")
    private final Model mainChatModel;

    private final PromptLoader prompts;

    public ReActAgent build() {
        return ReActAgent.builder()
                .name("interview_report_agent")
                .model(mainChatModel)
                .sysPrompt(prompts.load("prompts/interviewer-report.txt"))
                .memory(new InMemoryMemory())
                .build();
    }
}
