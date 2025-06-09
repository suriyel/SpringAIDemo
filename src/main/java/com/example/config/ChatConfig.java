package com.example.config;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Spring AI Alibaba 配置类
 *
 * @author AI Assistant
 */
@Configuration
public class ChatConfig {

    /**
     * 配置内存聊天记录仓库
     *
     * @return InMemoryChatMemoryRepository
     */
    @Bean
    public InMemoryChatMemoryRepository chatMemoryRepository() {
        return new InMemoryChatMemoryRepository();
    }

    /**
     * 配置消息窗口聊天内存
     * 保留最近20条消息的历史记录
     *
     * @param repository 聊天记录仓库
     * @return MessageWindowChatMemory
     */
    @Bean
    public ChatMemory chatMemory(InMemoryChatMemoryRepository repository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(20)  // 最大保存20条消息
                .build();
    }

    /**
     * 配置RestClient.Builder，设置更长的超时时间
     *
     * @return RestClient.Builder
     */
    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder()
                .requestInterceptor((request, body, execution) -> {
                    // 可以在这里添加请求拦截逻辑，比如重试
                    return execution.execute(request, body);
                });
    }

    /**
     * 配置全局ChatClient，避免重复创建和添加Advisors
     *
     * @param chatClientBuilder ChatClient构建器
     * @param chatMemory 聊天内存
     * @return 配置好的ChatClient
     */
    @Bean
    public ChatClient globalChatClient(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory) {
        return chatClientBuilder
                .defaultSystem("你是一个博学的智能聊天助手，请根据用户提问回答！")
                // 全局配置Memory Advisor，使用默认conversationId，后续通过context动态指定
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                // 全局配置Logger Advisor
                .defaultAdvisors(new SimpleLoggerAdvisor())
                // 设置默认Options参数，使用更快的模型减少超时
                .defaultOptions(DashScopeChatOptions.builder()
                        .withModel("qwen-turbo")  // 使用更快的turbo模型
                        .withTemperature(0.7)
                        .withTopP(0.8)
                        .withMaxToken(1024)      // 减少token数量加快响应
                        .build())
                .build();
    }
}