package com.resume.agent.matcher;

import com.resume.config.PromptLoader;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.Model;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * JD 匹配 Agent Builder - 使用 light 模型（分类/分析任务）
 */
@Component
@RequiredArgsConstructor
public class JDMatcherAgentBuilder {

    @Qualifier("deepSeekModel")
    private final Model lightChatModel;

    private final PromptLoader prompts;

    public ReActAgent build() {
        return ReActAgent.builder()
                .name("jd_matcher_agent")
                .model(lightChatModel)
                .sysPrompt(prompts.load("prompts/jd-matcher.txt"))
                .memory(new InMemoryMemory())
                .build();
    }
}
