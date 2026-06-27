package com.resume.agent.interviewer;

import com.resume.model.dto.InterviewMessage;
import com.resume.model.entity.UserProfile;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 面试上下文 - 维护单次面试会话的状态
 */
@Data
public class InterviewContext {

    /**
     * 面试阶段状态机
     */
    public enum Stage {
        OPENING,        // 开场（自我介绍）
        TECH_QUESTIONS, // 技术提问
        FOLLOW_UP,      // 追问深入
        REVERSE_QA,     // 反问环节
        CLOSING,        // 结束
        COMPLETED       // 已完成
    }

    private Long interviewId;
    private Long userId;
    private String resumeContent;
    private String jdContent;
    private UserProfile userProfile;
    private Stage currentStage;
    private List<InterviewMessage> messages;
    private List<String> askedQuestions;
    private int questionCount;
    private int maxQuestions = 8;

    public InterviewContext() {
        this.currentStage = Stage.OPENING;
        this.messages = new ArrayList<>();
        this.askedQuestions = new ArrayList<>();
        this.questionCount = 0;
    }

    public void addMessage(InterviewMessage message) {
        this.messages.add(message);
    }

    public void incrementQuestionCount() {
        this.questionCount++;
    }

    public boolean isMaxQuestionsReached() {
        return this.questionCount >= this.maxQuestions;
    }
}
