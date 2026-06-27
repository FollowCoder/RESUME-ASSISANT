package com.resume.agent.interviewer;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 面试上下文内存存储
 */
@Component
public class InterviewContextStore {

    private final ConcurrentHashMap<Long, InterviewContext> contexts = new ConcurrentHashMap<>();

    public InterviewContext get(Long interviewId) {
        return contexts.get(interviewId);
    }

    public void put(Long interviewId, InterviewContext context) {
        contexts.put(interviewId, context);
    }

    public void remove(Long interviewId) {
        contexts.remove(interviewId);
    }

    public boolean contains(Long interviewId) {
        return contexts.containsKey(interviewId);
    }
}
