package com.resume.agent.interviewer;

import com.resume.config.PromptLoader;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.Model;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 面试官 Agent Builder - 使用 main 模型（面向用户的最终文案）
 * 五要素规范：
 * 1. name: 唯一名称（snake_case）
 * 2. model: main 模型（文字质量要求高）
 * 3. sysPrompt: 外部文件 Prompt
 * 4. memory: 每次 build 都 new InMemoryMemory()
 * 5. 工具: 不挂工具，纯对话 Agent
 */
@Component
@RequiredArgsConstructor
public class InterviewerAgentBuilder {

    @Qualifier("mainChatModel")
    private final Model mainChatModel;

    private final PromptLoader prompts;

    public ReActAgent build() {
        return ReActAgent.builder()
                .name("interviewer_agent")
                .model(mainChatModel)
                .sysPrompt(prompts.load("prompts/interviewer.txt"))
                .memory(new InMemoryMemory())
                .build();
    }
}
