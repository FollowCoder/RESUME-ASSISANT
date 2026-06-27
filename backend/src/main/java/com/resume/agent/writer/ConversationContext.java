package com.resume.agent.writer;

import com.resume.model.dto.ChatMessage;
import com.resume.model.dto.ResumeContent;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 对话上下文 - 维护简历编写对话的状态
 */
@Data
public class ConversationContext {

    private String sessionId;
    private Long userId;
    private String currentStage;
    private List<ChatMessage> history;
    private ResumeContent partialContent;
    private long lastAccessTime;

    public ConversationContext() {
        this.currentStage = Stage.GREETING.name();
        this.history = new ArrayList<>();
        this.partialContent = ResumeContent.builder()
                .education(new ArrayList<>())
                .workExperience(new ArrayList<>())
                .projectExperience(new ArrayList<>())
                .skills(new ArrayList<>())
                .build();
        this.lastAccessTime = System.currentTimeMillis();
    }

    public ConversationContext(String sessionId, Long userId) {
        this();
        this.sessionId = sessionId;
        this.userId = userId;
    }

    public void addMessage(ChatMessage message) {
        this.history.add(message);
        this.lastAccessTime = System.currentTimeMillis();
    }

    public void advanceStage() {
        Stage current = Stage.valueOf(this.currentStage);
        Stage[] stages = Stage.values();
        int nextIndex = current.ordinal() + 1;
        if (nextIndex < stages.length) {
            this.currentStage = stages[nextIndex].name();
        }
    }

    public boolean isCompleted() {
        return Stage.COMPLETE.name().equals(this.currentStage);
    }

    /**
     * 对话阶段枚举
     */
    public enum Stage {
        GREETING,       // 问候并了解需求
        BASIC_INFO,     // 基本信息
        EDUCATION,      // 教育背景
        WORK,           // 工作经历
        PROJECT,        // 项目经验
        SKILLS,         // 技能列表
        SUMMARY,        // 个人总结
        COMPLETE        // 完成
    }
}
