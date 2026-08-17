package com.example.aidemo.service;

import com.example.aidemo.model.IngestRequest;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库问答服务：RAG 流水线 + 多轮记忆（手动检索增强，零额外依赖）。
 * 流程：向量检索 -> 拼接上下文 -> ChatClient 生成回答（带会话记忆）。
 */
@Service
public class RagService {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    public RagService(VectorStore vectorStore, ChatClient.Builder builder, ChatMemory chatMemory) {
        this.vectorStore = vectorStore;
        this.chatMemory = chatMemory;
        this.chatClient = builder.build();
    }

    /** 文档入库：切片后写入内存向量库（SimpleVectorStore）。 */
    public String ingest(String content) {
        if (content == null || content.isBlank()) {
            return "内容为空，未入库。";
        }
        List<Document> docs = split(content).stream()
                .map(text -> Document.builder().text(text).build())
                .collect(Collectors.toList());
        vectorStore.add(docs);
        return "已入库 " + docs.size() + " 个文本块（约 500 字/块）。";
    }

    /** 知识库问答：检索增强 + 多轮记忆。 */
    public String answer(String question, String conversationId) {
        String convId = (conversationId == null || conversationId.isBlank()) ? "default" : conversationId;

        // 1) 向量检索（Top-4）
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder().query(question).topK(4).build());

        // 2) 拼接上下文
        String context = docs.stream()
                .map(Document::getText)
                .filter(t -> t != null && !t.isBlank())
                .collect(Collectors.joining("\n---\n"));

        // 3) 组装提示词
        String userPrompt = context.isBlank()
                ? question
                : "请仅基于下面的「知识库内容」回答用户问题，若知识库未覆盖请如实说明。\n\n" +
                  "【知识库内容】\n" + context + "\n\n【用户问题】" + question;

        // 4) 多轮记忆 + 生成
        MessageChatMemoryAdvisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
        return chatClient.prompt()
                .user(userPrompt)
                .advisors(memoryAdvisor)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, convId))
                .call()
                .content();
    }

    /** 简单等长切片（~500 字/块），演示用。 */
    private List<String> split(String content) {
        List<String> chunks = new java.util.ArrayList<>();
        final int max = 500;
        for (int i = 0; i < content.length(); i += max) {
            chunks.add(content.substring(i, Math.min(content.length(), i + max)));
        }
        return chunks;
    }
}
