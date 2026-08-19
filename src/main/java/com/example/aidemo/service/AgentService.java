package com.example.aidemo.service;

import com.example.aidemo.tools.KnowledgeTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.stereotype.Service;

/**
 * 知识库问答 Agent：基于 Tool Calling 的多轮 Agent。
 * 通过注册自定义工具（知识库检索、文档计数），
 * 让模型自主决定调用哪些工具来完成问答任务。
 */
@Service
public class AgentService {

    private static final String SYSTEM_PROMPT = """
            你是一名专业的知识库问答助手，擅长利用工具从知识库中检索证据来回答问题。
            你的目标是帮助用户基于知识库内容获取准确、可追溯的答案。
            你可以使用以下工具：
            1) searchKnowledgeBase：在知识库中检索与问题相关的文档片段，用于获取回答依据；
            2) countKnowledgeBase：查看知识库当前已入库的文档块数量。
            工作步骤：
            - 先理解用户问题，必要时调用 searchKnowledgeBase 获取相关片段；
            - 基于检索到的内容组织答案，并标注信息来源（片段编号）；
            - 若知识库未覆盖该问题，如实说明无法回答，不要编造。
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
