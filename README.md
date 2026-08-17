# agentforge · 知识库问答

> 面向 **AI Agent 开发岗** 的实战项目：基于 **Spring AI + spring-ai-alibaba** 落地一条完整的
> RAG 流水线与一个 **Tool Calling** 多轮 Agent，可直接写进知识库作为「AI Agent 工程落地」证据。

## 技术栈

| 能力 | 选型 | 说明 |
| --- | --- | --- |
| 框架 | Spring Boot 3.5.8 |  |
| AI 编排 | spring-ai-alibaba-bom 1.1.2.2 (Spring AI 1.1.x) | 阿里生态，统一管理 Spring AI 版本 |
| 对话模型 | DeepSeek `deepseek-chat` | 走 OpenAI 兼容协议（`spring-ai-starter-model-openai`） |
| 向量化 | 本地 ONNX (`spring-ai-starter-model-transformers`) | 规避 DeepSeek 无 embedding 接口的痛点，零外部服务 |
| 向量库 | `SimpleVectorStore`（内存） | 进程内检索，零依赖，演示友好 |
| 记忆 | `MessageWindowChatMemory` | 多轮对话，保留最近 20 条消息 |

## 架构与能力

```
文本 ──▶ 切片 ──▶ ONNX Embedding ──▶ SimpleVectorStore（内存）
                                      │
用户提问 ──▶ MessageChatMemoryAdvisor（多轮记忆）
            + 手动向量检索（similaritySearch, Top-4）
            + DeepSeek ChatModel ──▶ 检索增强回答（RAG）

知识库优化任务 ──▶ ChatClient（系统提示：AI Agent 招聘顾问）
                + Tool Calling：getJobDescription / extractKeywords / saveKnowledgeDoc
                ──▶ 自主编排的知识库优化 Agent
```

- **RAG 流水线**：文档入库 → 切片 → 本地向量化 → 内存向量库 → 检索增强生成。
- **Tool Calling Agent**：模型自主决定调用 JD 检索、关键词提取、结果落库等工具。
- **多轮对话记忆**：按 `conversationId` 隔离，支持连续追问。

## 快速开始

```bash
# 1. 配置 DeepSeek API Key（不要硬编码）
export DEEPSEEK_API_KEY=sk-你的key

# 2. 编译（需 JDK 17+，本机用 JDK 22 验证通过）
mvn clean package

# 3. 运行
java -jar target/agentforge-0.0.1-SNAPSHOT.jar
```

> 首次运行会从远程下载 ONNX 模型（约 80MB）并缓存到
> `${java.io.tmpdir}/spring-ai-onnx-model`。国内网络慢可改用 `application.yml` 中注释的
> HuggingFace 国内镜像。

## 接口示例

```bash
# 健康检查
curl http://localhost:8080/api/health

# 文档入库（写入知识库）
curl -X POST http://localhost:8080/api/doc/ingest \
  -H 'Content-Type: application/json' \
  -d '{"content":"Spring AI 提供 ChatClient 统一抽象，支持 RAG、Tool Calling 与多轮记忆。"}'

# 知识库问答
curl -X POST http://localhost:8080/api/chat \
  -H 'Content-Type: application/json' \
  -d '{"question":"Spring AI 支持哪些核心能力？","conversationId":"c1"}'

# 知识库优化 Agent
curl -X POST http://localhost:8080/api/agent \
  -H 'Content-Type: application/json' \
  -d '{"task":"帮我针对 AI Agent 开发岗优化知识库，并提取我的关键词","conversationId":"c2"}'

# 读取最近保存的优化知识库
curl http://localhost:8080/api/doc/latest
```

## 知识库话术要点（面试可讲）

- 用 **Spring AI + DeepSeek** 落地 RAG：自研切片 + 本地 ONNX 向量化 + 内存向量库，
  绕开了「DeepSeek 无 embedding 接口」的工程约束。
- 用 **Tool Calling** 把「JD 匹配 / 关键词分析 / 结果落库」拆成可调用工具，
  由模型自主编排，体现对 Agent 范式（而非仅 Prompt）的理解。
- 全程 **多轮对话记忆**，按会话隔离，体现生产级工程意识。

## 后续可扩展（线2-B）

- 把 `SimpleVectorStore` 换成 `PgVector` / `RedisVectorStore` 做持久化。
- 引入 spring-ai-alibaba 的 **多智能体编排**（Supervisor / Pipeline / Debate）。
- 接入真实知识库 PDF 解析（Apache PDFBox）打通端到端知识库优化。
