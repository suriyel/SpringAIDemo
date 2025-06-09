package com.example.controller;

import com.example.service.ChatService;
import com.example.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG功能控制器
 * 提供文档管理和RAG对话的REST API接口
 *
 * @author AI Assistant
 */
@Slf4j
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RAGController {

    private final ChatService chatService;
    private final DocumentService documentService;

    /**
     * RAG对话接口
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chatWithRAG(
            @RequestParam String message,
            @RequestParam(defaultValue = "default") String sessionId) {

        try {
            long startTime = System.currentTimeMillis();
            String response = chatService.chatWithRAG(sessionId, message);
            long duration = System.currentTimeMillis() - startTime;

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("response", response);
            result.put("sessionId", sessionId);
            result.put("processingTime", duration + "ms");
            result.put("mode", "RAG");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("RAG对话处理失败", e);
            return ResponseEntity.status(500).body(createErrorResponse("RAG对话失败: " + e.getMessage()));
        }
    }

    /**
     * 智能对话接口 - 自动选择是否使用RAG
     */
    @PostMapping("/smart-chat")
    public ResponseEntity<Map<String, Object>> smartChat(
            @RequestParam String message,
            @RequestParam(defaultValue = "default") String sessionId) {

        try {
            long startTime = System.currentTimeMillis();
            String response = chatService.smartChat(sessionId, message);
            long duration = System.currentTimeMillis() - startTime;

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("response", response);
            result.put("sessionId", sessionId);
            result.put("processingTime", duration + "ms");
            result.put("mode", "SMART");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("智能对话处理失败", e);
            return ResponseEntity.status(500).body(createErrorResponse("智能对话失败: " + e.getMessage()));
        }
    }

    /**
     * 按类别进行RAG对话
     */
    @PostMapping("/chat/category")
    public ResponseEntity<Map<String, Object>> chatWithRAGByCategory(
            @RequestParam String message,
            @RequestParam String category,
            @RequestParam(defaultValue = "default") String sessionId) {

        try {
            long startTime = System.currentTimeMillis();
            String response = chatService.chatWithRAGByCategory(sessionId, message, category);
            long duration = System.currentTimeMillis() - startTime;

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("response", response);
            result.put("sessionId", sessionId);
            result.put("category", category);
            result.put("processingTime", duration + "ms");
            result.put("mode", "RAG_CATEGORY");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("分类RAG对话处理失败", e);
            return ResponseEntity.status(500).body(createErrorResponse("分类RAG对话失败: " + e.getMessage()));
        }
    }

    /**
     * 文档上传接口
     */
    @PostMapping("/documents/upload")
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String category) {

        try {
            String result = documentService.uploadAndProcessDocument(file, category);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", result);
            response.put("filename", file.getOriginalFilename());
            response.put("category", category);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("文档上传失败", e);
            return ResponseEntity.status(500).body(createErrorResponse("文档上传失败: " + e.getMessage()));
        }
    }

    /**
     * 批量文档上传接口
     */
    @PostMapping("/documents/upload-batch")
    public ResponseEntity<Map<String, Object>> uploadMultipleDocuments(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(required = false) String category) {

        try {
            Map<String, String> results = documentService.uploadMultipleDocuments(files, category);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("results", results);
            response.put("totalFiles", files.size());
            response.put("category", category);

            long successCount = results.values().stream()
                    .filter(result -> !result.contains("失败"))
                    .count();
            response.put("successCount", successCount);
            response.put("failureCount", files.size() - successCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("批量文档上传失败", e);
            return ResponseEntity.status(500).body(createErrorResponse("批量文档上传失败: " + e.getMessage()));
        }
    }

    /**
     * 添加文本文档接口
     */
    @PostMapping("/documents/add-text")
    public ResponseEntity<Map<String, Object>> addTextDocument(
            @RequestParam String content,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String category) {

        try {
            String result = documentService.addTextDocument(content, title, category);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", result);
            response.put("title", title);
            response.put("category", category);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("文本文档添加失败", e);
            return ResponseEntity.status(500).body(createErrorResponse("文本文档添加失败: " + e.getMessage()));
        }
    }

    /**
     * 文档搜索接口
     */
    @GetMapping("/documents/search")
    public ResponseEntity<Map<String, Object>> searchDocuments(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int maxResults) {

        try {
            List<Document> documents = documentService.searchDocuments(query, maxResults);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("query", query);
            response.put("totalResults", documents.size());
            response.put("documents", documents.stream().map(this::documentToMap).toList());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("文档搜索失败", e);
            return ResponseEntity.status(500).body(createErrorResponse("文档搜索失败: " + e.getMessage()));
        }
    }

    /**
     * 按类别搜索文档接口
     */
    @GetMapping("/documents/search/category")
    public ResponseEntity<Map<String, Object>> searchDocumentsByCategory(
            @RequestParam String query,
            @RequestParam String category,
            @RequestParam(defaultValue = "10") int maxResults) {

        try {
            List<Document> documents = documentService.searchDocumentsByCategory(query, category, maxResults);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("query", query);
            response.put("category", category);
            response.put("totalResults", documents.size());
            response.put("documents", documents.stream().map(this::documentToMap).toList());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("分类文档搜索失败", e);
            return ResponseEntity.status(500).body(createErrorResponse("分类文档搜索失败: " + e.getMessage()));
        }
    }

    /**
     * 文档相关性分析接口
     */
    @GetMapping("/documents/analyze")
    public ResponseEntity<Map<String, Object>> analyzeDocumentRelevance(@RequestParam String query) {
        try {
            String analysis = chatService.analyzeDocumentRelevance(query);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("query", query);
            response.put("analysis", analysis);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("文档相关性分析失败", e);
            return ResponseEntity.status(500).body(createErrorResponse("文档相关性分析失败: " + e.getMessage()));
        }
    }

    /**
     * 获取文档统计信息接口
     */
    @GetMapping("/documents/stats")
    public ResponseEntity<Map<String, Object>> getDocumentStats() {
        try {
            Map<String, Object> stats = documentService.getDocumentStats();
            stats.put("success", true);
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("获取文档统计信息失败", e);
            return ResponseEntity.status(500).body(createErrorResponse("获取文档统计信息失败: " + e.getMessage()));
        }
    }

    /**
     * 获取RAG系统状态接口
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getRAGSystemStatus() {
        try {
            String status = chatService.getRAGSystemStatus();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("status", status);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("获取RAG系统状态失败", e);
            return ResponseEntity.status(500).body(createErrorResponse("获取RAG系统状态失败: " + e.getMessage()));
        }
    }

    /**
     * 清除所有文档接口 (谨慎使用)
     */
    @DeleteMapping("/documents/clear-all")
    public ResponseEntity<Map<String, Object>> clearAllDocuments() {
        try {
            String result = documentService.clearAllDocuments();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", result);
            response.put("warning", "所有文档已被清除，此操作不可恢复");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("清除所有文档失败", e);
            return ResponseEntity.status(500).body(createErrorResponse("清除文档失败: " + e.getMessage()));
        }
    }

    /**
     * 重置对话会话接口
     */
    @PostMapping("/chat/reset")
    public ResponseEntity<Map<String, Object>> resetConversation(
            @RequestParam(defaultValue = "default") String sessionId) {

        try {
            chatService.startNewConversation(sessionId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "会话已重置");
            response.put("sessionId", sessionId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("重置会话失败", e);
            return ResponseEntity.status(500).body(createErrorResponse("会话重置失败: " + e.getMessage()));
        }
    }

    /**
     * 获取会话历史接口
     */
    @GetMapping("/chat/history")
    public ResponseEntity<Map<String, Object>> getConversationHistory(
            @RequestParam(defaultValue = "default") String sessionId) {

        try {
            var history = chatService.getConversationHistory(sessionId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("sessionId", sessionId);
            response.put("messageCount", history.size());
            response.put("history", history);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("获取会话历史失败", e);
            return ResponseEntity.status(500).body(createErrorResponse("获取会话历史失败: " + e.getMessage()));
        }
    }

    // 私有辅助方法

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }

    private Map<String, Object> documentToMap(Document document) {
        Map<String, Object> docMap = new HashMap<>();
        docMap.put("content", document.getFormattedContent());
        docMap.put("metadata", document.getMetadata());

        // 截取内容摘要（前200个字符）
        String content = document.getFormattedContent();
        String summary = content.length() > 200 ? content.substring(0, 200) + "..." : content;
        docMap.put("summary", summary);

        return docMap;
    }
}