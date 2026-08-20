package com.example.aidemo.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class AiConfig {

    /**
     * 内存向量库：SimpleVectorStore，进程内检索，零外部依赖。
     * 默认 profile 生效，适合本地快速开发（无需 Redis）。
     * 生产/演示持久化请改用 redis profile（配合 docker-compose 中的 redis-stack），
     * 由 Spring AI 自动装配 RedisVectorStore，重启不丢数据。
     */
    @Bean
    @Profile("!redis")
    public VectorStore inMemoryVectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    /**
     * 多轮对话记忆：保留最近 20 条消息，按会话隔离。
     */
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder().maxMessages(20).build();
    }
}
