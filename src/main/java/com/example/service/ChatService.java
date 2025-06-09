package com.example.service;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 聊天服务类
 * 实现多轮对话功能，对接阿里云通义模型
 *
 * @author AI Assistant
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    @Autowired
    @Qualifier("globalChatClient")
    private ChatClient chatClient;

    @Autowired
    private ChatMemory chatMemory;

    /**
     * 发送消息并获取回复（默认会话）
     *
     * @param userMessage 用户消息
     * @return AI回复
     */
    public String chat(String userMessage) {
        return chat("default", userMessage);
    }

    /**
     * 发送消息并获取回复（指定会话）
     *
     * @param sessionId 会话ID
     * @param userMessage 用户消息
     * @return AI回复
     */
    public String chat(String sessionId, String userMessage) {
        try {
            log.debug("收到用户消息 [会话:{}]: {}", sessionId, userMessage);

            // 通过context动态设置conversationId，使用全局ChatClient
            String response = chatClient
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
     * 发送消息并获取回复（带自定义选项）
     *
     * @param sessionId 会话ID
     * @param userMessage 用户消息
     * @param options 自定义选项
     * @return AI回复
     */
    public String chat(String sessionId, String userMessage, DashScopeChatOptions options) {
        try {
            log.debug("收到用户消息 [会话:{}, 自定义选项]: {}", sessionId, userMessage);

            // 通过context动态设置conversationId，并应用自定义选项
            String response = chatClient
                    .prompt(userMessage)
                    .options(options)
                    .advisors(spec -> spec.param("CHAT_MEMORY_CONVERSATION_ID", sessionId))
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
     *
     * @param sessionId 会话ID
     */
    public void startNewConversation(String sessionId) {
        log.info("开始新对话 [会话:{}]", sessionId);
        // 清除Spring AI管理的内存
        chatMemory.clear(sessionId);
    }

    /**
     * 开始新的对话（默认会话）
     */
    public void startNewConversation() {
        startNewConversation("default");
    }

    /**
     * 获取对话历史（从Spring AI ChatMemory获取）
     *
     * @param sessionId 会话ID
     * @return 对话历史
     */
    public List<Message> getConversationHistory(String sessionId) {
        return chatMemory.get(sessionId);
    }

    /**
     * 获取对话历史（默认会话）
     *
     * @return 对话历史
     */
    public List<Message> getConversationHistory() {
        return getConversationHistory("default");
    }

    /**
     * 获取会话信息
     *
     * @param sessionId 会话ID
     * @return 会话信息字符串
     */
    public String getSessionInfo(String sessionId) {
        List<Message> messages = chatMemory.get(sessionId);
        int messageCount = messages.size();
        int conversationRounds = messageCount / 2; // 每轮对话包含1个用户消息和1个助手消息
        return String.format("会话ID: %s, 消息数量: %d, 对话轮次: %d", sessionId, messageCount, conversationRounds);
    }

    /**
     * 获取所有会话信息
     *
     * @return 所有会话信息
     */
    public String getAllSessionsInfo() {
        // 注意：InMemoryChatMemoryRepository没有直接获取所有会话的方法
        // 这里返回一个简单的描述
        return "Spring AI管理的会话（具体数量需要通过其他方式统计）";
    }
}