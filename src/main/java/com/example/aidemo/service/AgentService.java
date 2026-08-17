package com.example.aidemo.service;

import com.example.aidemo.tools.KnowledgeTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.stereotype.Service;

/**
 * 知识库优化 Agent：基于 Tool Calling 的多轮 Agent。
 * 通过注册自定义工具（JD 模板检索、关键词提取、结果落库），
 * 让模型自主决定调用哪些工具来完成知识库优化任务。
 */
@Service
public class AgentService {

    private static final String SYSTEM_PROMPT = """
            你是一名资深的 AI Agent 技术招聘顾问，同时精通知识库优化。
            你的目标是帮助候选人针对「AI Agent 开发」岗位优化知识库。
            你可以使用以下工具：
            1) getJobDescription：获取目标岗位的 JD 模板，用于匹配能力项；
            2) extractKeywords：从文本中提取高频关键词，用于关键词匹配度分析；
            3) saveKnowledgeDoc：把最终优化后的知识库全文保存下来。
            工作步骤：
            - 先调用 getJobDescription 明确岗位能力要求；
            - 再调用 extractKeywords 分析候选人现状与 JD 的差距；
            - 最后给出优化建议，并在用户确认后调用 saveKnowledgeDoc 落库。
            回答要结构化、专业、可量化，避免空话。
            """;

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    public AgentService(ChatClient.Builder chatClientBuilder,
                        ChatMemory chatMemory,
                        KnowledgeTools knowledgeTools) {
        this.chatMemory = chatMemory;
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(knowledgeTools)
                .build();
    }

    /** 运行一次 Agent 任务（模型可自主多轮调用工具）。 */
    public String run(String task, String conversationId) {
        String convId = (conversationId == null || conversationId.isBlank()) ? "default" : conversationId;
        MessageChatMemoryAdvisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();

        return chatClient.prompt()
                .user(task)
                .advisors(memoryAdvisor)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, convId))
                .call()
                .content();
    }
}
