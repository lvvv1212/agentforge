package com.example.aidemo.tools;

import com.example.aidemo.service.RagService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 知识库 Agent 的自定义工具集（领域中立，用于演示 Tool Calling 编排）。
 * 每个 @Tool 方法都会被模型在运行时按需调用。
 */
@Component
public class KnowledgeTools {

    private final RagService ragService;

    public KnowledgeTools(RagService ragService) {
        this.ragService = ragService;
    }

    @Tool(description = "在知识库中检索与问题相关的文档片段（最多 4 条），用于回答前先获取证据")
    public String searchKnowledgeBase(@ToolParam(description = "检索关键词或问题") String query) {
        return ragService.search(query, 4);
    }

    @Tool(description = "返回知识库当前已入库的文档块数量，用于确认知识库是否已灌入内容")
    public String countKnowledgeBase() {
        return ragService.countDocs();
    }
}
