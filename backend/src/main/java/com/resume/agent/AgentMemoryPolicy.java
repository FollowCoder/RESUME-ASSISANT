package com.resume.agent;

import io.agentscope.core.ReActAgent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Agent 内存策略 - 按 Agent 角色差异化管理内存
 * - 面试官/面试报告：每轮清空，防止历史污染当前判断
 * - 简历优化/匹配分析：保留少量上下文，保证对话连贯性
 */
@Slf4j
@Component
public class AgentMemoryPolicy {

    /**
     * 面试官 Agent：每轮清空，只靠 Prompt 注入的历史摘要做上下文
     */
    public void beforeInterviewerCall(ReActAgent agent) {
        if (agent.getMemory() != null) {
            agent.getMemory().clear();
        }
    }

    /**
     * 面试报告 Agent：每轮清空，报告生成是独立任务
     */
    public void beforeReportCall(ReActAgent agent) {
        if (agent.getMemory() != null) {
            agent.getMemory().clear();
        }
    }

    /**
     * 匹配分析 Agent：每轮清空，分析是独立任务
     */
    public void beforeMatchCall(ReActAgent agent) {
        if (agent.getMemory() != null) {
            agent.getMemory().clear();
        }
    }

    /**
     * 简历优化 Agent：每轮清空，优化是独立任务
     */
    public void beforeOptimizeCall(ReActAgent agent) {
        if (agent.getMemory() != null) {
            agent.getMemory().clear();
        }
    }

    /**
     * 简历写作 Agent：保留最近 4 轮（8 条），对话需要连贯性
     */
    public void beforeWriterCall(ReActAgent agent) {
        trimToLast(agent, 8);
    }

    /**
     * 通用裁切：保留最后 limit 条消息
     */
    private void trimToLast(ReActAgent agent, int limit) {
        if (agent.getMemory() == null) return;
        int size = agent.getMemory().getMessages().size();
        if (size <= limit) return;
        for (int i = 0; i < size - limit; i++) {
            agent.getMemory().deleteMessage(0);
        }
    }
}
