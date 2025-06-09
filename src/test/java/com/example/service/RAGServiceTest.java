package com.example.service;

import com.example.SpringAiChatApplication;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RAG功能集成测试类
 * 使用TestContainers进行完整的RAG功能测试
 *
 * @author AI Assistant
 */
@Slf4j
@SpringBootTest(classes = SpringAiChatApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RAGServiceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
            .withDatabaseName("ai_knowledge_db_test")
            .withUsername("postgres")
            .withPassword("postgres")
            .withInitScript("init-db.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.ai.vectorstore.pgvector.initialize-schema", () -> "true");
    }

    @Autowired
    private ChatService chatService;

    @Autowired
    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        log.info("测试环境准备中...");
        // 每个测试前清理会话
        chatService.startNewConversation("rag_test_session");
    }

    @Test
    @Order(1)
    void testDatabaseConnection() {
        log.info("=== 开始数据库连接测试 ===");

        assertTrue(postgres.isRunning(), "PostgreSQL容器应该正在运行");
        assertNotNull(documentService, "DocumentService应该被正确注入");
        assertNotNull(chatService, "ChatService应该被正确注入");

        log.info("数据库连接测试通过");
        log.info("=== 数据库连接测试完成 ===");
    }

    @Test
    @Order(2)
    void testAddTextDocuments() {
        log.info("=== 开始文本文档添加测试 ===");

        try {
            // 添加Spring AI相关文档
            String springAiDoc = """
                Spring AI是一个为AI工程提供Spring生态系统设计模式的应用框架。
                它提供了与多种AI模型提供商的集成，包括OpenAI、Azure OpenAI、Amazon Bedrock等。
                Spring AI的核心特性包括：
                1. 可移植的API：支持多种AI模型提供商
                2. 聊天客户端：用于与聊天模型交互
                3. 嵌入模型：用于文本向量化
                4. 向量存储：支持多种向量数据库
                5. RAG支持：检索增强生成功能
                6. 函数调用：允许AI模型调用外部函数
                """;

            String result1 = documentService.addTextDocument(springAiDoc, "Spring AI介绍", "技术文档");
            assertNotNull(result1);
            assertTrue(result1.contains("添加成功"));
            log.info("Spring AI文档添加结果: {}", result1);

            // 添加RAG相关文档
            String ragDoc = """
                检索增强生成(RAG)是一种将信息检索与语言生成相结合的技术。
                RAG的工作流程包括：
                1. 文档预处理：将文档分块并向量化
                2. 向量存储：将向量存储在向量数据库中
                3. 检索阶段：根据用户查询检索相关文档
                4. 生成阶段：结合检索到的上下文生成回答
                RAG的优势：
                - 提供实时、准确的信息
                - 减少模型幻觉
                - 支持领域特定知识
                - 可解释性强
                """;

            String result2 = documentService.addTextDocument(ragDoc, "RAG技术介绍", "技术文档");
            assertNotNull(result2);
            assertTrue(result2.contains("添加成功"));
            log.info("RAG文档添加结果: {}", result2);

            // 添加公司政策文档
            String policyDoc = """
                公司远程工作政策：
                1. 员工可以申请远程工作，需要提前一周申请
                2. 远程工作期间需要保持在线状态
                3. 每周至少有两天需要到办公室工作
                4. 远程工作期间需要参加所有必要的会议
                5. 工作时间为上午9点到下午6点
                6. 如有紧急情况，需要在1小时内响应
                """;

            String result3 = documentService.addTextDocument(policyDoc, "远程工作政策", "公司政策");
            assertNotNull(result3);
            assertTrue(result3.contains("添加成功"));
            log.info("政策文档添加结果: {}", result3);

            log.info("=== 文本文档添加测试完成 ===");

        } catch (Exception e) {
            log.error("文本文档添加测试失败", e);
            fail("文本文档添加测试失败: " + e.getMessage());
        }
    }

    @Test
    @Order(3)
    void testDocumentSearch() {
        log.info("=== 开始文档搜索测试 ===");

        try {
            // 搜索Spring AI相关文档
            List<Document> springAiResults = documentService.searchDocuments("Spring AI特性", 5);
            assertFalse(springAiResults.isEmpty(), "应该找到Spring AI相关文档");
            log.info("Spring AI搜索结果数量: {}", springAiResults.size());

            // 搜索RAG相关文档
            List<Document> ragResults = documentService.searchDocuments("检索增强生成", 5);
            assertFalse(ragResults.isEmpty(), "应该找到RAG相关文档");
            log.info("RAG搜索结果数量: {}", ragResults.size());

            // 按类别搜索
            List<Document> techDocs = documentService.searchDocumentsByCategory("API", "技术文档", 5);
            log.info("技术文档类别搜索结果数量: {}", techDocs.size());

            List<Document> policyDocs = documentService.searchDocumentsByCategory("远程工作", "公司政策", 5);
            assertFalse(policyDocs.isEmpty(), "应该找到公司政策相关文档");
            log.info("公司政策类别搜索结果数量: {}", policyDocs.size());

            log.info("=== 文档搜索测试完成 ===");

        } catch (Exception e) {
            log.error("文档搜索测试失败", e);
            fail("文档搜索测试失败: " + e.getMessage());
        }
    }

    @Test
    @Order(4)
    void testBasicRAGChat() {
        log.info("=== 开始基础RAG对话测试 ===");

        try {
            // 测试Spring AI相关问题
            String question1 = "Spring AI的核心特性有哪些？";
            String answer1 = chatService.chatWithRAG("rag_test_session", question1);

            assertNotNull(answer1);
            assertFalse(answer1.trim().isEmpty());
            log.info("问题1: {}", question1);
            log.info("回答1: {}", answer1);

            // 测试RAG相关问题
            String question2 = "什么是检索增强生成？它有什么优势？";
            String answer2 = chatService.chatWithRAG("rag_test_session", question2);

            assertNotNull(answer2);
            assertFalse(answer2.trim().isEmpty());
            log.info("问题2: {}", question2);
            log.info("回答2: {}", answer2);

            // 测试公司政策相关问题
            String question3 = "公司的远程工作政策是什么？";
            String answer3 = chatService.chatWithRAG("rag_test_session", question3);

            assertNotNull(answer3);
            assertFalse(answer3.trim().isEmpty());
            log.info("问题3: {}", question3);
            log.info("回答3: {}", answer3);

            log.info("=== 基础RAG对话测试完成 ===");

        } catch (Exception e) {
            log.error("基础RAG对话测试失败", e);
            fail("基础RAG对话测试失败: " + e.getMessage());
        }
    }

    @Test
    @Order(5)
    void testCategorySpecificRAG() {
        log.info("=== 开始分类RAG对话测试 ===");

        try {
            // 在技术文档类别中搜索
            String techQuestion = "向量存储的作用是什么？";
            String techAnswer = chatService.chatWithRAGByCategory("rag_test_session", techQuestion, "技术文档");

            assertNotNull(techAnswer);
            assertFalse(techAnswer.trim().isEmpty());
            log.info("技术问题: {}", techQuestion);
            log.info("技术回答: {}", techAnswer);

            // 在公司政策类别中搜索
            String policyQuestion = "远程工作需要满足什么条件？";
            String policyAnswer = chatService.chatWithRAGByCategory("rag_test_session", policyQuestion, "公司政策");

            assertNotNull(policyAnswer);
            assertFalse(policyAnswer.trim().isEmpty());
            log.info("政策问题: {}", policyQuestion);
            log.info("政策回答: {}", policyAnswer);

            // 测试不存在的类别
            String unknownCategoryAnswer = chatService.chatWithRAGByCategory("rag_test_session", "测试问题", "不存在的类别");
            assertTrue(unknownCategoryAnswer.contains("没有找到"), "应该提示没有找到相关文档");
            log.info("未知类别测试: {}", unknownCategoryAnswer);

            log.info("=== 分类RAG对话测试完成 ===");

        } catch (Exception e) {
            log.error("分类RAG对话测试失败", e);
            fail("分类RAG对话测试失败: " + e.getMessage());
        }
    }

    @Test
    @Order(6)
    void testSmartChat() {
        log.info("=== 开始智能对话测试 ===");

        try {
            // 测试应该使用RAG的问题（存在相关文档）
            String ragQuestion = "Spring AI支持哪些向量数据库？";
            String ragAnswer = chatService.smartChat("smart_test_session", ragQuestion);

            assertNotNull(ragAnswer);
            assertFalse(ragAnswer.trim().isEmpty());
            log.info("RAG问题: {}", ragQuestion);
            log.info("RAG回答: {}", ragAnswer);

            // 测试应该使用普通对话的问题（不存在相关文档）
            String generalQuestion = "今天天气怎么样？";
            String generalAnswer = chatService.smartChat("smart_test_session", generalQuestion);

            assertNotNull(generalAnswer);
            assertFalse(generalAnswer.trim().isEmpty());
            log.info("一般问题: {}", generalQuestion);
            log.info("一般回答: {}", generalAnswer);

            log.info("=== 智能对话测试完成 ===");

        } catch (Exception e) {
            log.error("智能对话测试失败", e);
            fail("智能对话测试失败: " + e.getMessage());
        }
    }

    @Test
    @Order(7)
    void testDocumentRelevanceAnalysis() {
        log.info("=== 开始文档相关性分析测试 ===");

        try {
            String query = "人工智能和机器学习";
            String analysis = chatService.analyzeDocumentRelevance(query);

            assertNotNull(analysis);
            assertFalse(analysis.trim().isEmpty());
            log.info("查询: {}", query);
            log.info("相关性分析: {}", analysis);

            log.info("=== 文档相关性分析测试完成 ===");

        } catch (Exception e) {
            log.error("文档相关性分析测试失败", e);
            fail("文档相关性分析测试失败: " + e.getMessage());
        }
    }

    @Test
    @Order(8)
    void testMultiTurnRAGConversation() {
        log.info("=== 开始多轮RAG对话测试 ===");

        try {
            String sessionId = "multiturn_rag_session";

            // 第一轮：询问Spring AI
            String question1 = "请介绍一下Spring AI";
            String answer1 = chatService.chatWithRAG(sessionId, question1);
            assertNotNull(answer1);
            log.info("第1轮 - 问题: {}", question1);
            log.info("第1轮 - 回答: {}", answer1.substring(0, Math.min(200, answer1.length())));

            // 第二轮：追问具体特性
            String question2 = "它的向量存储功能是怎样的？";
            String answer2 = chatService.chatWithRAG(sessionId, question2);
            assertNotNull(answer2);
            log.info("第2轮 - 问题: {}", question2);
            log.info("第2轮 - 回答: {}", answer2.substring(0, Math.min(200, answer2.length())));

            // 第三轮：询问应用场景
            String question3 = "这些功能可以用在什么场景？";
            String answer3 = chatService.chatWithRAG(sessionId, question3);
            assertNotNull(answer3);
            log.info("第3轮 - 问题: {}", question3);
            log.info("第3轮 - 回答: {}", answer3.substring(0, Math.min(200, answer3.length())));

            // 验证会话历史
            var history = chatService.getConversationHistory(sessionId);
            assertEquals(6, history.size(), "应该有6条消息（3问3答）");

            log.info("=== 多轮RAG对话测试完成 ===");

        } catch (Exception e) {
            log.error("多轮RAG对话测试失败", e);
            fail("多轮RAG对话测试失败: " + e.getMessage());
        }
    }

    @Test
    @Order(9)
    void testDocumentStats() {
        log.info("=== 开始文档统计信息测试 ===");

        try {
            Map<String, Object> stats = documentService.getDocumentStats();
            assertNotNull(stats);

            log.info("文档统计信息:");
            stats.forEach((key, value) -> log.info("  {}: {}", key, value));

            String systemStatus = chatService.getRAGSystemStatus();
            assertNotNull(systemStatus);
            log.info("系统状态: {}", systemStatus);

            log.info("=== 文档统计信息测试完成 ===");

        } catch (Exception e) {
            log.error("文档统计信息测试失败", e);
            fail("文档统计信息测试失败: " + e.getMessage());
        }
    }

    @Test
    @Order(10)
    void testRAGPerformance() {
        log.info("=== 开始RAG性能测试 ===");

        try {
            String question = "Spring AI的主要优势是什么？";

            // 测试普通对话性能
            long startTime1 = System.currentTimeMillis();
            String normalAnswer = chatService.chat("performance_test", question);
            long normalTime = System.currentTimeMillis() - startTime1;

            assertNotNull(normalAnswer);
            log.info("普通对话耗时: {}ms", normalTime);

            // 测试RAG对话性能
            long startTime2 = System.currentTimeMillis();
            String ragAnswer = chatService.chatWithRAG("performance_test", question);
            long ragTime = System.currentTimeMillis() - startTime2;

            assertNotNull(ragAnswer);
            log.info("RAG对话耗时: {}ms", ragTime);

            // 比较回答质量（长度作为简单指标）
            log.info("普通回答长度: {} 字符", normalAnswer.length());
            log.info("RAG回答长度: {} 字符", ragAnswer.length());

            // 通常RAG回答应该更详细、更准确
            log.info("性能比较: RAG耗时比普通对话多 {}ms", ragTime - normalTime);

            log.info("=== RAG性能测试完成 ===");

        } catch (Exception e) {
            log.error("RAG性能测试失败", e);
            fail("RAG性能测试失败: " + e.getMessage());
        }
    }

    @AfterEach
    void tearDown() {
        log.debug("测试清理...");
    }

    @AfterAll
    static void cleanup() {
        log.info("所有RAG测试完成，容器将被清理");
    }
}