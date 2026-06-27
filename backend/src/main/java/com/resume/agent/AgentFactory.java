package com.resume.agent;

import com.resume.agent.interviewer.InterviewReportAgentBuilder;
import com.resume.agent.interviewer.InterviewerAgentBuilder;
import com.resume.agent.matcher.JDMatcherAgentBuilder;
import com.resume.agent.optimizer.ResumeOptimizerAgentBuilder;
import com.resume.agent.writer.ResumeWriterAgentBuilder;

import io.agentscope.core.ReActAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;

/**
 * Agent 工厂 - 会话级 Agent 缓存
 * 设计要点：
 * 1. 每个 session 持有一组 Agent 实例，复用 InMemoryMemory 累积上下文
 * 2. LRU 自动淘汰，避免内存泄漏
 * 3. 使用 Java record 做不可变聚合
 */
@Component
@RequiredArgsConstructor
public class AgentFactory {

    private final InterviewerAgentBuilder interviewerBuilder;
    private final InterviewReportAgentBuilder reportBuilder;
    private final JDMatcherAgentBuilder matcherBuilder;
    private final ResumeOptimizerAgentBuilder optimizerBuilder;
    private final ResumeWriterAgentBuilder writerBuilder;

    // LRU 缓存，最大 1000 个会话
    private final Map<String, AgentSet> cache = Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, AgentSet> e) {
                    return size() > 1000;
                }
            });

    /**
     * 获取或创建会话级 Agent 集合
     */
    public AgentSet get(String sessionId) {
        return cache.computeIfAbsent(sessionId, sid -> new AgentSet(
                interviewerBuilder.build(),
                reportBuilder.build(),
                matcherBuilder.build(),
                optimizerBuilder.build(),
                writerBuilder.build()
        ));
    }

    /**
     * 移除会话（当面试或写作完成后清理）
     */
    public void remove(String sessionId) {
        cache.remove(sessionId);
    }

    /**
     * Agent 集合 - 不可变聚合
     */
    public record AgentSet(
            ReActAgent interviewer,
            ReActAgent report,
            ReActAgent matcher,
            ReActAgent optimizer,
            ReActAgent writer
    ) {}
}
