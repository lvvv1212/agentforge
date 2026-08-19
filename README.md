# agentforge · 知识库问答 Agent

> 面向 **AI Agent 开发** 的实战项目：基于 **Spring AI + spring-ai-alibaba** 落地一条完整的
> RAG 流水线，并用 **Tool Calling** 编排一个可自主调用知识库工具的多轮 Agent。

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

Agent 任务 ──▶ ChatClient（系统提示：知识库问答助手）
            + Tool Calling：searchKnowledgeBase / countKnowledgeBase
            ──▶ 自主编排的知识库问答 Agent
```

- **RAG 流水线**：文档入库 → 切片 → 本地向量化 → 内存向量库 → 检索增强生成。
- **Tool Calling Agent**：模型自主决定调用知识库检索、文档计数等工具，体现对 Agent 范式（而非仅 Prompt）的理解。
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

# 知识库问答（RAG + 多轮记忆）
curl -X POST http://localhost:8080/api/chat \
  -H 'Content-Type: application/json' \
  -d '{"question":"Spring AI 支持哪些核心能力？","conversationId":"c1"}'

# 知识库问答 Agent（Tool Calling 自主编排）
curl -X POST http://localhost:8080/api/agent \
  -H 'Content-Type: application/json' \
  -d '{"task":"帮我查一下知识库里关于 Spring AI 核心能力的内容，并总结要点","conversationId":"c2"}'
```

## 自定义演示数据

仓库不含任何业务数据，你可以用自己的文档做演示：把文本 POST 到 `/api/doc/ingest`
灌入知识库，再用 `/api/chat` 或 `/api/agent` 验证检索增强问答。例如：

```bash
# 灌入一段你自己的文档内容
curl -X POST http://localhost:8080/api/doc/ingest \
  -H 'Content-Type: application/json' \
  -d '{"content":"这里放你自己的文档内容……"}'
```

## 后续可扩展

- 把 `SimpleVectorStore` 换成 `PgVector` / `RedisVectorStore` 做持久化。
- 引入 spring-ai-alibaba 的 **多智能体编排**（Supervisor / Pipeline / Debate）。
- 为 Agent 增加更多领域中立工具（如文档摘要、实体抽取）。
