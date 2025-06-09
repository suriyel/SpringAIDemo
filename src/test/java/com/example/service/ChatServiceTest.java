package com.example.service;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.example.SpringAiChatApplication;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ChatService 测试类
 * 通过JUnit测试展示Spring AI Alibaba多轮对话功能
 *
 * @author AI Assistant
 */
@Slf4j
@SpringBootTest(classes = SpringAiChatApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ChatServiceTest {

    @Autowired
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        // 每个测试前清理对话历史
        chatService.startNewConversation();
        log.info("测试环境已准备就绪");
    }

    /**
     * 测试网络连接
     */
    @Test
    void testNetworkConnection() {
        log.info("=== 开始网络连接测试 ===");

        try {
            String response = chatService.chat("你好");

            assertNotNull(response);
            assertFalse(response.trim().isEmpty());

            log.info("网络连接测试成功");
            log.info("响应: {}", response);

        } catch (Exception e) {
            log.error("网络连接测试失败: {}", e.getMessage());
            log.error("请检查以下项目:");
            log.error("1. API Key是否正确设置: {}", System.getenv("AI_DASHSCOPE_API_KEY") != null ? "已设置" : "未设置");
            log.error("2. 网络是否可以访问 dashscope.aliyuncs.com");
            log.error("3. 防火墙是否阻止了HTTPS连接");
            log.error("4. 是否需要配置代理");

            fail("网络连接失败，请查看日志了解详细信息");
        }

        log.info("=== 网络连接测试完成 ===");
    }

    /**
     * 测试单轮对话
     */
    @Test
    void testSingleTurnChat() {
        log.info("=== 开始单轮对话测试 ===");

        try {
            String userMessage = "你好，请简单介绍一下你自己";
            String response = chatService.chat(userMessage);

            assertNotNull(response);
            assertFalse(response.trim().isEmpty());

            log.info("用户: {}", userMessage);
            log.info("AI: {}", response);

            // 验证对话历史
            List<Message> history = chatService.getConversationHistory();
            assertEquals(2, history.size()); // 一问一答

            log.info("=== 单轮对话测试完成 ===");

        } catch (Exception e) {
            log.error("单轮对话测试失败: {}", e.getMessage());
            if (e.getMessage().contains("timeout")) {
                log.error("网络超时，建议:");
                log.error("1. 检查网络连接");
                log.error("2. 尝试使用更快的模型 (qwen-turbo)");
                log.error("3. 减少maxTokens设置");
            }
            throw e;
        }
    }

    /**
     * 测试多轮对话
     */
    @Test
    void testMultiTurnChat() throws InterruptedException {
        log.info("=== 开始多轮对话测试 ===");

        try {
            // 第一轮对话 - 使用简短问题
            String message1 = "你好";
            String response1 = chatService.chat(message1);

            assertNotNull(response1);
            log.info("第1轮 - 用户: {}", message1);
            log.info("第1轮 - AI: {}", response1.substring(0, Math.min(100, response1.length())));

            // 等待一秒避免请求过快
            Thread.sleep(1000);

            // 第二轮对话（上下文相关）
            String message2 = "你叫什么名字？";
            String response2 = chatService.chat(message2);

            assertNotNull(response2);
            log.info("第2轮 - 用户: {}", message2);
            log.info("第2轮 - AI: {}", response2.substring(0, Math.min(100, response2.length())));

            // 等待一秒
            Thread.sleep(1000);

            // 第三轮对话（继续上下文）
            String message3 = "你能做什么？";
            String response3 = chatService.chat(message3);

            assertNotNull(response3);
            log.info("第3轮 - 用户: {}", message3);
            log.info("第3轮 - AI: {}", response3.substring(0, Math.min(100, response3.length())));

            // 验证对话历史包含所有轮次
            List<Message> history = chatService.getConversationHistory();
            assertEquals(6, history.size()); // 三问三答

            log.info("对话历史总数: {}", history.size());
            log.info("=== 多轮对话测试完成 ===");

        } catch (Exception e) {
            log.error("多轮对话测试失败: {}", e.getMessage());
            if (e.getMessage().contains("timeout")) {
                log.error("建议优化措施:");
                log.error("1. 使用更短的问题");
                log.error("2. 增加请求间隔");
                log.error("3. 检查网络稳定性");
            }
            throw e;
        }
    }

    /**
     * 测试多会话场景
     */
    @Test
    void testMultiSessionChat() {
        log.info("=== 开始多会话测试 ===");

        // 会话1
        String session1 = "user_001";
        String message1_1 = "请帮我计算 25 + 37";
        String response1_1 = chatService.chat(session1, message1_1);

        log.info("会话1 - 用户: {}", message1_1);
        log.info("会话1 - AI: {}", response1_1);

        // 会话2
        String session2 = "user_002";
        String message2_1 = "今天天气怎么样？";
        String response2_1 = chatService.chat(session2, message2_1);

        log.info("会话2 - 用户: {}", message2_1);
        log.info("会话2 - AI: {}", response2_1);

        // 继续会话1
        String message1_2 = "那么 62 减去刚才的结果是多少？";
        String response1_2 = chatService.chat(session1, message1_2);

        log.info("会话1续 - 用户: {}", message1_2);
        log.info("会话1续 - AI: {}", response1_2);

        // 验证不同会话的历史记录是独立的
        List<Message> history1 = chatService.getConversationHistory(session1);
        List<Message> history2 = chatService.getConversationHistory(session2);

        assertEquals(4, history1.size()); // 会话1: 两问两答
        assertEquals(2, history2.size()); // 会话2: 一问一答

        log.info("会话1历史数: {}, 会话2历史数: {}", history1.size(), history2.size());
        log.info("=== 多会话测试完成 ===");
    }

    /**
     * 测试对话记忆功能
     */
    @Test
    void testConversationMemory() {
        log.info("=== 开始对话记忆测试 ===");

        // 建立上下文
        String message1 = "我的名字是张三，我是一名软件工程师";
        String response1 = chatService.chat(message1);

        log.info("建立上下文 - 用户: {}", message1);
        log.info("建立上下文 - AI: {}", response1);

        // 测试是否记住之前的信息
        String message2 = "请问我的职业是什么？";
        String response2 = chatService.chat(message2);

        log.info("测试记忆 - 用户: {}", message2);
        log.info("测试记忆 - AI: {}", response2);

        // 验证AI是否能够回忆起之前的信息
        assertNotNull(response2);
        // 注意：具体的内容验证取决于模型的实际表现，这里主要验证功能流程

        log.info("=== 对话记忆测试完成 ===");
    }

    /**
     * 测试自定义ChatOptions
     */
    @Test
    void testCustomChatOptions() {
        log.info("=== 开始自定义选项测试 ===");

        // 创建自定义选项 - 更高的创造性
        DashScopeChatOptions creativeOptions = DashScopeChatOptions.builder()
                .withModel("qwen-plus")
                .withTemperature(0.9)  // 更高的创造性
                .withTopP(0.9)
                .withMaxToken(1024)
                .build();

        String userMessage = "给我写一首关于春天的诗";
        String response = chatService.chat("creative_session", userMessage, creativeOptions);

        assertNotNull(response);
        assertFalse(response.trim().isEmpty());

        log.info("自定义选项对话 - 用户: {}", userMessage);
        log.info("自定义选项对话 - AI: {}", response);

        log.info("=== 自定义选项测试完成 ===");
    }

    /**
     * 测试对话重置功能
     */
    @Test
    void testConversationReset() {
        log.info("=== 开始对话重置测试 ===");

        // 先进行一些对话
        chatService.chat("第一条消息");
        chatService.chat("第二条消息");

        List<Message> historyBefore = chatService.getConversationHistory();
        assertEquals(4, historyBefore.size());
        log.info("重置前对话历史数: {}", historyBefore.size());

        // 重置对话
        chatService.startNewConversation();

        // 验证历史已清空
        List<Message> historyAfter = chatService.getConversationHistory();
        assertEquals(0, historyAfter.size());
        log.info("重置后对话历史数: {}", historyAfter.size());

        // 新对话应该正常工作
        String newMessage = "这是重置后的第一条消息";
        String newResponse = chatService.chat(newMessage);

        assertNotNull(newResponse);
        log.info("重置后新对话 - 用户: {}", newMessage);
        log.info("重置后新对话 - AI: {}", newResponse);

        log.info("=== 对话重置测试完成 ===");
    }

    /**
     * 测试会话信息查询
     */
    @Test
    void testSessionInfo() {
        log.info("=== 开始会话信息测试 ===");

        String sessionId = "test_session";

        // 进行几轮对话
        chatService.chat(sessionId, "消息1");
        chatService.chat(sessionId, "消息2");
        chatService.chat(sessionId, "消息3");

        // 获取会话信息
        String sessionInfo = chatService.getSessionInfo(sessionId);
        String allSessionsInfo = chatService.getAllSessionsInfo();

        log.info("会话信息: {}", sessionInfo);
        log.info("所有会话信息: {}", allSessionsInfo);

        assertNotNull(sessionInfo);
        assertNotNull(allSessionsInfo);

        log.info("=== 会话信息测试完成 ===");
    }

    /**
     * 测试通义千问模型的特殊功能
     */
    @Test
    void testQwenSpecialFeatures() {
        log.info("=== 开始通义千问特殊功能测试 ===");

        // 测试数学计算能力
        String mathQuestion = "请计算 123 * 456 = ?";
        String mathResponse = chatService.chat("math_session", mathQuestion);

        log.info("数学计算 - 用户: {}", mathQuestion);
        log.info("数学计算 - AI: {}", mathResponse);

        // 测试中文理解能力
        String chineseQuestion = "请解释一下'落红不是无情物，化作春泥更护花'这句诗的含义";
        String chineseResponse = chatService.chat("chinese_session", chineseQuestion);

        log.info("中文理解 - 用户: {}", chineseQuestion);
        log.info("中文理解 - AI: {}", chineseResponse);

        assertNotNull(mathResponse);
        assertNotNull(chineseResponse);

        log.info("=== 通义千问特殊功能测试完成 ===");
    }
}