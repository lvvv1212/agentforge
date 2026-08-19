package com.example.aidemo.controller;

import com.example.aidemo.model.AgentRequest;
import com.example.aidemo.model.ChatRequest;
import com.example.aidemo.model.IngestRequest;
import com.example.aidemo.service.AgentService;
import com.example.aidemo.service.RagService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AiController {

    private final RagService ragService;
    private final AgentService agentService;

    public AiController(RagService ragService, AgentService agentService) {
        this.ragService = ragService;
        this.agentService = agentService;
    }

    /** 健康检查 */
    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("status", "UP");
        m.put("service", "agentforge");
        return m;
    }

    /** 文档入库：把文本写入内存向量库（RAG 知识库） */
    @PostMapping("/doc/ingest")
    public Map<String, String> ingest(@RequestBody IngestRequest req) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("result", ragService.ingest(req.content()));
        return m;
    }

    /** 知识库问答（RAG + 多轮记忆） */
    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody ChatRequest req) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("answer", ragService.answer(req.question(), req.conversationId()));
        return m;
    }

    /** 知识库问答 Agent（Tool Calling 编排） */
    @PostMapping("/agent")
    public Map<String, String> agent(@RequestBody AgentRequest req) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("result", agentService.run(req.task(), req.conversationId()));
        return m;
    }
}
