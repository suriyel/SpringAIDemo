package com.example.config;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Spring AI Alibaba + RAG 配置类
 * 集成聊天记忆、向量存储和检索增强生成功能
 *
 * @author AI Assistant
 */
@Configuration
public class ChatConfig {

    @Value("${app.rag.similarity-threshold:0.75}")
    private double similarityThreshold;

    @Value("${app.rag.top-k:5}")
    private int topK;

    @Value("${app.rag.chunk-size:1000}")
    private int chunkSize;

    @Value("${app.rag.chunk-overlap:200}")
    private int chunkOverlap;

    @Value("${app.rag.enable-rewrite-query:true}")
    private boolean enableRewriteQuery;

    /**
     * 配置内存聊天记录仓库
     */
    @Bean
    public InMemoryChatMemoryRepository chatMemoryRepository() {
        return new InMemoryChatMemoryRepository();
    }

    /**
     * 配置消息窗口聊天内存
     * 保留最近20条消息的历史记录
     */
    @Bean
    public ChatMemory chatMemory(InMemoryChatMemoryRepository repository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(20)  // 最大保存20条消息
                .build();
    }

    /**
     * 配置文档分割器
     * 用于将长文档分割成适合向量化的小块
     */
    @Bean
    public TokenTextSplitter textSplitter() {
        return new TokenTextSplitter(chunkSize, chunkOverlap, 5, 10000, true);
    }

    /**
     * 配置向量存储文档检索器
     */
    @Bean
    public VectorStoreDocumentRetriever documentRetriever(VectorStore vectorStore) {
        return VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(similarityThreshold)
                .topK(topK)
                .build();
    }

    /**
     * 配置基础问答Advisor (简单RAG实现)
     */
    @Bean
    public QuestionAnswerAdvisor questionAnswerAdvisor(VectorStore vectorStore) {
        return new QuestionAnswerAdvisor(vectorStore);
    }

    /**
     * 配置检索增强生成Advisor (高级RAG实现)
     */
    @Bean
    public RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(
            VectorStoreDocumentRetriever documentRetriever,
            ChatClient.Builder chatClientBuilder) {

        var builder = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever);

        // 如果启用查询重写，添加查询转换器
        if (enableRewriteQuery) {
            builder.queryTransformers(
                    RewriteQueryTransformer.builder()
                            .chatClientBuilder(chatClientBuilder)
                            .build()
            );
        }

        return builder.build();
    }

    /**
     * 配置RestClient.Builder，设置更长的超时时间
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
     * 配置全局ChatClient，集成聊天记忆和RAG功能
     */
    @Bean
    public ChatClient globalChatClient(
            ChatClient.Builder chatClientBuilder,
            ChatMemory chatMemory) {
        return chatClientBuilder
                .defaultSystem("""
                    你是一个博学的智能聊天助手，能够根据提供的上下文信息和用户问题进行回答。
                    请遵循以下原则：
                    1. 优先使用提供的上下文信息来回答问题
                    2. 如果上下文信息不足，请明确说明并基于你的知识进行补充
                    3. 回答要准确、简洁且有帮助
                    4. 如果无法确定答案，请诚实说明
                    """)
                // 全局配置Memory Advisor
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                // 全局配置Logger Advisor
                .defaultAdvisors(new SimpleLoggerAdvisor())
                // 设置默认Options参数
                .defaultOptions(DashScopeChatOptions.builder()
                        .withModel("qwen-plus")  // 使用qwen-plus模型获得更好的推理能力
                        .withTemperature(0.7)
                        .withTopP(0.8)
                        .withMaxToken(2048)      // 增加token数量以支持更长的上下文
                        .build())
                .build();
    }

    /**
     * 配置专用RAG ChatClient
     * 默认启用RAG功能的ChatClient
     */
    @Bean("ragChatClient")
    public ChatClient ragChatClient(
            ChatClient.Builder chatClientBuilder,
            ChatMemory chatMemory,
            RetrievalAugmentationAdvisor retrievalAugmentationAdvisor) {
        return chatClientBuilder
                .defaultSystem("""
                    你是一个基于知识库的智能助手，专门回答基于已有文档和知识的问题。
                    请严格根据提供的上下文信息来回答问题，如果上下文中没有相关信息，请明确表示无法从知识库中找到相关信息。
                    """)
                // 配置RAG功能
                .defaultAdvisors(retrievalAugmentationAdvisor)
                // 配置聊天记忆
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                // 配置日志记录
                .defaultAdvisors(new SimpleLoggerAdvisor())
                // 针对RAG优化的参数
                .defaultOptions(DashScopeChatOptions.builder()
                        .withModel("qwen-plus")
                        .withTemperature(0.3)    // 降低创造性，提高准确性
                        .withTopP(0.9)
                        .withMaxToken(3072)      // 更大的token支持更多上下文
                        .build())
                .build();
    }
}