package com.resume.config;

import io.agentscope.core.formatter.dashscope.DashScopeMultiAgentFormatter;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.OpenAIChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AgentScope 模型配置 - 双模型分层策略
 * - 主模型（mainChatModel）：面向用户的最终文案（质量优先）
 * - 轻模型（lightChatModel）：分类/抽取/追问等 NLP 任务（速度优先）
 * - 多 Agent 模型（multiAgentChatModel）：群组讨论专用
 */
@Configuration
public class AgentScopeConfig {

    @Value("${agentscope.dashscope.api-key}")
    private String apiKey;

    @Bean("mainChatModel")
    public Model mainChatModel() {
        return DashScopeChatModel.builder()
                .apiKey(apiKey)
                .modelName("qwen-max")
                .build();
    }

    @Bean("lightChatModel")
    public Model lightChatModel() {
        return DashScopeChatModel.builder()
                .apiKey(apiKey)
                .modelName("qwen-turbo")
                .build();
    }

    @Bean("multiAgentChatModel")
    public Model multiAgentChatModel() {
        return DashScopeChatModel.builder()
                .apiKey(apiKey)
                .modelName("qwen-plus")
                .formatter(new DashScopeMultiAgentFormatter())
                .build();
    }
    @Bean("deepSeekModel")
    public Model deepSeekModel(){
       return OpenAIChatModel.builder()
                .apiKey(apiKey)
                .modelName("deepseek-v4-pro")
                .baseUrl("https://api.deepseek.com")
                .build();
    }

}
