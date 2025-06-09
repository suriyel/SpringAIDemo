package com.example.service;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 增强的聊天服务类
 * 实现多轮对话功能，集成RAG检索增强生成功能
 *
 * @author AI Assistant
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    @Autowired
    @Qualifier("globalChatClient")
    private ChatClient globalChatClient;

    @Autowired
    @Qualifier("ragChatClient")
    private ChatClient ragChatClient;

    @Autowired
    private ChatMemory chatMemory;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private QuestionAnswerAdvisor questionAnswerAdvisor;

    @Autowired
    private RetrievalAugmentationAdvisor retrievalAugmentationAdvisor;

    /**
     * 发送消息并获取回复（默认会话，不使用RAG）
     */
    public String chat(String userMessage) {
        return chat("default", userMessage);
    }

    /**
     * 发送消息并获取回复（指定会话，不使用RAG）
     */
    public String chat(String sessionId, String userMessage) {
        try {
            log.debug("收到用户消息 [会话:{}]: {}", sessionId, userMessage);

            String response = globalChatClient
                    .prompt(userMessage)
                    .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, sessionId))
                    .call()
                    .content();

            log.debug("AI回复 [会话:{}]: {}", sessionId, response);
            return response;

        } catch (Exception e) {
            log.error("调用AI模型时发生错误 [会话:{}]", sessionId, e);
            throw new RuntimeException("AI服务调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 使用RAG进行对话（默认会话）
     */
    public String chatWithRAG(String userMessage) {
        return chatWithRAG("default", userMessage);
    }

    /**
     * 使用RAG进行对话（指定会话）
     */
    public String chatWithRAG(String sessionId, String userMessage) {
        try {
            log.debug("收到RAG查询 [会话:{}]: {}", sessionId, userMessage);

            String response = ragChatClient
                    .prompt(userMessage)
                    .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, sessionId))
                    .call()
                    .content();

            log.debug("RAG回复 [会话:{}]: {}", sessionId, response);
            return response;

        } catch (Exception e) {
            log.error("RAG查询时发生错误 [会话:{}]", sessionId, e);
            throw new RuntimeException("RAG服务调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 使用RAG进行对话，指定文档类别
     */
    public String chatWithRAGByCategory(String sessionId, String userMessage, String category) {
        try {
            log.debug("收到分类RAG查询 [会话:{}, 类别:{}]: {}", sessionId, category, userMessage);

            // 先搜索特定类别的相关文档
            List<Document> relevantDocs = documentService.searchDocumentsByCategory(userMessage, category, 5);

            if (relevantDocs.isEmpty()) {
                return String.format("在类别 '%s' 中没有找到与您的问题相关的文档。请尝试调整问题或选择其他类别。", category);
            }

            // 构建包含上下文的提示
            String contextualPrompt = buildContextualPrompt(userMessage, relevantDocs);

            String response = globalChatClient
                    .prompt(contextualPrompt)
                    .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, sessionId))
                    .call()
                    .content();

            log.debug("分类RAG回复 [会话:{}, 类别:{}]: {}", sessionId, category, response);
            return response;

        } catch (Exception e) {
            log.error("分类RAG查询时发生错误 [会话:{}, 类别:{}]", sessionId, category, e);
            throw new RuntimeException("分类RAG服务调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 智能对话模式 - 自动判断是否需要使用RAG
     */
    public String smartChat(String sessionId, String userMessage) {
        try {
            log.debug("智能对话模式 [会话:{}]: {}", sessionId, userMessage);

            // 先尝试搜索相关文档
            List<Document> relevantDocs = documentService.searchDocuments(userMessage, 3);

            if (!relevantDocs.isEmpty()) {
                // 如果找到相关文档，使用RAG模式
                log.debug("找到 {} 个相关文档，使用RAG模式", relevantDocs.size());
                return chatWithRAG(sessionId, userMessage);
            } else {
                // 如果没有找到相关文档，使用普通对话模式
                log.debug("未找到相关文档，使用普通对话模式");
                return chat(sessionId, userMessage);
            }

        } catch (Exception e) {
            log.error("智能对话时发生错误 [会话:{}]", sessionId, e);
            // 发生错误时降级到普通对话模式
            return chat(sessionId, userMessage);
        }
    }

    /**
     * 获取文档相关性分析
     */
    public String analyzeDocumentRelevance(String query) {
        try {
            List<Document> relevantDocs = documentService.searchDocuments(query, 10);

            if (relevantDocs.isEmpty()) {
                return "没有找到与查询相关的文档。";
            }

            StringBuilder analysis = new StringBuilder();
            analysis.append(String.format("找到 %d 个相关文档：\n\n", relevantDocs.size()));

            for (int i = 0; i < relevantDocs.size(); i++) {
                Document doc = relevantDocs.get(i);
                analysis.append(String.format("%d. ", i + 1));

                // 添加文档元数据信息
                if (doc.getMetadata().containsKey("source_file")) {
                    analysis.append(String.format("来源: %s", doc.getMetadata().get("source_file")));
                }
                if (doc.getMetadata().containsKey("category")) {
                    analysis.append(String.format(" | 类别: %s", doc.getMetadata().get("category")));
                }

                // 添加文档内容摘要（前200个字符）
                String content = doc.getFormattedContent();
                String summary = content.length() > 200 ? content.substring(0, 200) + "..." : content;
                analysis.append(String.format("\n   摘要: %s\n\n", summary));
            }

            return analysis.toString();

        } catch (Exception e) {
            log.error("分析文档相关性时发生错误", e);
            return "文档相关性分析失败: " + e.getMessage();
        }
    }

    /**
     * 发送消息并获取回复（带自定义选项）
     */
    public String chat(String sessionId, String userMessage, DashScopeChatOptions options) {
        try {
            log.debug("收到用户消息 [会话:{}, 自定义选项]: {}", sessionId, userMessage);

            String response = globalChatClient
                    .prompt(userMessage)
                    .options(options)
                    .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, sessionId))
                    .call()
                    .content();

            log.debug("AI回复 [会话:{}]: {}", sessionId, response);
            return response;

        } catch (Exception e) {
            log.error("调用AI模型时发生错误 [会话:{}]", sessionId, e);
            throw new RuntimeException("AI服务调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 开始新的对话（清除历史记录）
     */
    public void startNewConversation(String sessionId) {
        log.info("开始新对话 [会话:{}]", sessionId);
        chatMemory.clear(sessionId);
    }

    /**
     * 开始新的对话（默认会话）
     */
    public void startNewConversation() {
        startNewConversation("default");
    }

    /**
     * 获取对话历史
     */
    public List<Message> getConversationHistory(String sessionId) {
        return chatMemory.get(sessionId);
    }

    /**
     * 获取对话历史（默认会话）
     */
    public List<Message> getConversationHistory() {
        return getConversationHistory("default");
    }

    /**
     * 获取会话信息
     */
    public String getSessionInfo(String sessionId) {
        List<Message> messages = chatMemory.get(sessionId);
        int messageCount = messages.size();
        int conversationRounds = messageCount / 2;
        return String.format("会话ID: %s, 消息数量: %d, 对话轮次: %d", sessionId, messageCount, conversationRounds);
    }

    /**
     * 获取所有会话信息
     */
    public String getAllSessionsInfo() {
        return "Spring AI管理的会话（具体数量需要通过其他方式统计）";
    }

    /**
     * 获取RAG系统状态
     */
    public String getRAGSystemStatus() {
        try {
            var docStats = documentService.getDocumentStats();

            StringBuilder status = new StringBuilder();
            status.append("=== RAG系统状态 ===\n");
            status.append(String.format("文档总数: %s\n", docStats.getOrDefault("total_documents", "未知")));
            status.append(String.format("支持的文件类型: %s\n", docStats.get("supported_file_types")));

            if (docStats.containsKey("categories")) {
                status.append("文档类别分布:\n");
                @SuppressWarnings("unchecked")
                var categories = (java.util.Map<String, Long>) docStats.get("categories");
                categories.forEach((category, count) ->
                        status.append(String.format("  - %s: %d 个文档\n", category, count))
                );
            }

            status.append("RAG功能: 已启用\n");
            status.append("向量存储: PGVector\n");
            status.append("嵌入模型: OpenAI text-embedding-3-small");

            return status.toString();

        } catch (Exception e) {
            log.error("获取RAG系统状态时发生错误", e);
            return "RAG系统状态获取失败: " + e.getMessage();
        }
    }

    // 私有辅助方法

    private String buildContextualPrompt(String userMessage, List<Document> relevantDocs) {
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("根据以下相关文档内容回答问题：\n\n");

        contextBuilder.append("=== 相关文档 ===\n");
        for (int i = 0; i < relevantDocs.size(); i++) {
            Document doc = relevantDocs.get(i);
            contextBuilder.append(String.format("文档 %d:\n", i + 1));
            contextBuilder.append(doc.getContentFormatter());
            contextBuilder.append("\n\n");
        }

        contextBuilder.append("=== 用户问题 ===\n");
        contextBuilder.append(userMessage);
        contextBuilder.append("\n\n请根据上述文档内容回答问题。如果文档中没有相关信息，请说明无法从提供的文档中找到答案。");

        return contextBuilder.toString();
    }
}