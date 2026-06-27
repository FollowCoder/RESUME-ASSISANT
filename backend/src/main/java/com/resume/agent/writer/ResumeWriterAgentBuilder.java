package com.resume.agent.writer;

import com.resume.config.PromptLoader;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.Model;
import io.agentscope.core.skill.SkillBox;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 简历写作 Agent Builder - 使用 main 模型（对话式引导用户）
 * 集成 SkillBox 启用 tailored-resume-generator 技能
 */
@Component
@RequiredArgsConstructor
public class ResumeWriterAgentBuilder {

    @Qualifier("mainChatModel")
    private final Model mainChatModel;

    private final PromptLoader prompts;
    private final SkillBox skillBox;

    public ReActAgent build() {
        return ReActAgent.builder()
                .name("resume_writer_agent")
                .model(mainChatModel)
                .skillBox(skillBox)
                .sysPrompt(prompts.load("prompts/resume-writer.txt"))
                .memory(new InMemoryMemory())
                .build();
    }
}
