package com.resume.agent.optimizer;

import com.resume.config.PromptLoader;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.Model;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 简历优化 Agent Builder - 使用 main 模型（生成优化建议）
 */
@Component
@RequiredArgsConstructor
public class ResumeOptimizerAgentBuilder {

    @Qualifier("deepSeekModel")
    private final Model mainChatModel;

    private final PromptLoader prompts;

    public ReActAgent build() {
        return ReActAgent.builder()
                .name("resume_optimizer_agent")
                .model(mainChatModel)
                .sysPrompt(prompts.load("prompts/resume-optimizer.txt"))
                .memory(new InMemoryMemory())
                .build();
    }
}
