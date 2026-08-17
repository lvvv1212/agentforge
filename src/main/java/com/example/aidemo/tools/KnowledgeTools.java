package com.example.aidemo.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 知识库优化 Agent 的自定义工具集。
 * 每个 @Tool 方法都会被模型在运行时按需调用（Tool Calling）。
 */
@Component
public class KnowledgeTools {

    /** 进程内保存优化后的知识库（演示用；生产可替换为数据库 / 对象存储）。 */
    private final Map<String, String> docStore = new ConcurrentHashMap<>();

    /** JD 模板库：key 为岗位名，value 为能力要求描述。 */
    private static final Map<String, String> JD_LIBRARY = Map.of(
            "AI Agent 开发",
            "岗位：AI Agent 开发工程师。核心能力：1) 熟悉 LLM 应用开发（Prompt 工程、RAG、Fine-tuning）；" +
                    "2) 掌握 Agent 框架与编排（ReAct、Tool Calling、多智能体协作）；" +
                    "3) 有 Spring AI / LangChain 等工程落地经验；4) 熟悉向量数据库与 Embedding；" +
                    "5) 全栈能力（Java/Spring Boot + Vue/TS）可独立交付。",
            "产品经理",
            "岗位：AI 产品经理。核心能力：1) 需求洞察与 PRD 撰写；2) AI 功能定义与落地节奏；" +
                    "3) 数据驱动迭代；4) 跨端（Web/小程序）协同；5) 技术理解力（能与算法/工程团队对齐）。"
    );

    @Tool(description = "获取指定岗位的职位描述(JD)模板，用于匹配知识库能力项。支持：AI Agent 开发、产品经理")
    public String getJobDescription(@ToolParam(description = "岗位名称，如：AI Agent 开发") String role) {
        return JD_LIBRARY.getOrDefault(role,
                "暂未收录「" + role + "」的 JD 模板，可用：AI Agent 开发 / 产品经理");
    }

    @Tool(description = "从文本中提取高频关键词（按出现频次排序，最多 15 个），用于知识库关键词匹配度分析")
    public String extractKeywords(@ToolParam(description = "待分析文本") String text) {
        if (text == null || text.isBlank()) {
            return "文本为空。";
        }
        // 简单分词：按中文/英文标点与空白切分，过滤停用词与单字
        String[] raw = text.split("[\\s\\p{P}\\p{Z}]+");
        Map<String, Integer> freq = new HashMap<>();
        Set<String> stop = Set.of("的", "了", "和", "与", "及", "是", "在", "我", "你", "他",
                "她", "我们", "他们", "一个", "进行", "通过", "基于", "以及", "使用", "实现", "相关", "the", "a", "an", "and", "or", "to", "of");
        for (String w : raw) {
            w = w.trim();
            if (w.isEmpty() || w.length() < 2 || stop.contains(w)) {
                continue;
            }
            freq.merge(w, 1, Integer::sum);
        }
        List<Map.Entry<String, Integer>> list = new ArrayList<>(freq.entrySet());
        list.sort((a, b) -> b.getValue() - a.getValue());
        StringBuilder sb = new StringBuilder();
        list.stream().limit(15).forEach(e -> sb.append(e.getKey()).append("(").append(e.getValue()).append(") "));
        return sb.toString().trim();
    }

    @Tool(description = "保存最终优化后的知识库全文，返回保存确认（含字符长度）")
    public String saveKnowledgeDoc(@ToolParam(description = "优化后的知识库全文") String content) {
        docStore.put("latest", content);
        return "已保存优化知识库，字符长度=" + (content == null ? 0 : content.length());
    }

    /** 供 Controller 读取最近一次保存的知识库（非工具方法）。 */
    public String getLatestDoc() {
        return docStore.getOrDefault("latest", "暂无已保存的优化知识库。");
    }
}
