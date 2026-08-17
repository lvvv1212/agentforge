package com.example.aidemo.model;

/** Agent 任务请求（知识库优化 / 通用 Agent 编排） */
public record AgentRequest(String task, String conversationId) {
}
