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
| 向量库 | `SimpleVectorStore`（内存，默认）/ `RedisVectorStore`（`redis` profile，持久化） | 默认零依赖；切 `redis` profile + redis-stack 即持久化，重启不丢数据 |
| 记忆 | `MessageWindowChatMemory` | 多轮对话，保留最近 20 条消息 |

## 架构与能力

```
文本 ──▶ 切片 ──▶ ONNX Embedding ──▶ SimpleVectorStore（内存，默认）
                                      │        └─ 或 RedisVectorStore（redis profile，持久化）
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

### 持久化运行（Docker + Redis，重启不丢数据）

默认 `java -jar` 用的是内存向量库，进程一停数据就没了。想要真正的持久化
（以及演示用过向量数据库），用 `redis` profile 把向量存进 Redis：

```bash
# 方式一：Docker Compose（推荐，一条命令拉起 redis-stack + 应用）
export DEEPSEEK_API_KEY=sk-你的key
docker compose up -d --build

# 方式二：本地跑 redis-stack，再以 redis profile 启动 jar
docker run -d --name redis-stack -p 6379:6379 -v redis-data:/data redis/redis-stack-server:latest
REDIS_HOST=localhost java -jar target/agentforge-0.0.1-SNAPSHOT.jar --spring.profiles.active=redis
```

> 注意：RedisVectorStore 需要 Redis 带**向量检索能力**（`redis-stack-server` 或 Redis 8+），
> 普通 `redis` 镜像没有 `FT.CREATE` 命令会启动失败。向量数据存于 `redis-data` 卷，
> 应用重启/重建都不会丢失已入库的知识库。

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

> **为什么不内置示例数据**：知识库 Demo 的价值在于「换任何领域都能跑」，预置数据
> 反而把项目绑死在某个主题上、还涉及版权与原创性。所以这里刻意做成**领域中立**，
> 由使用者灌入自己的文档，既安全又最能体现 RAG 的通用性。

## 设计取舍（为什么这么做）

| 决策 | 理由 |
| --- | --- |
| 向量化用**本地 ONNX**，不调 DeepSeek embedding | DeepSeek 没有 embedding 接口；本地 all-MiniLM-L6-v2 零外部依赖、零成本、数据不出域，离线也能跑 |
| 默认**内存向量库**，另提供 **Redis 持久化** | 开发期零依赖秒启动；演示/生产用 `redis` profile 切 RedisVectorStore，既展示「用过向量数据库」又解决重启丢数据 |
| 用 **Tool Calling** 而非纯 Prompt | 让模型自主决定何时检索、是否计数，体现对 Agent 范式（工具编排）而非简单问答的理解 |
| API Key 走**环境变量**，不硬编码 | 避免密钥泄露进仓库，符合安全基线 |

## 后续可扩展

- 把 `RedisVectorStore` 换成 `PgVector`（PostgreSQL + pgvector 扩展），验证多种向量库切换成本。
- 引入 spring-ai-alibaba 的 **多智能体编排**（Supervisor / Pipeline / Debate）。
- 为 Agent 增加更多领域中立工具（如文档摘要、实体抽取）。
