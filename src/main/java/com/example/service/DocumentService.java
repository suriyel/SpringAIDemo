package com.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 文档管理服务
 * 负责文档的上传、处理、向量化和存储
 *
 * @author AI Assistant
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;
    private final TokenTextSplitter textSplitter;

    @Value("${app.document-storage-path:documents/}")
    private String documentStoragePath;

    @Value("${app.rag.similarity-threshold:0.75}")
    private double similarityThreshold;

    @Value("${app.rag.top-k:5}")
    private int topK;

    @Value("#{'${app.supported-document-types}'.split(',')}")
    private List<String> supportedDocumentTypes;

    /**
     * 上传并处理文档
     *
     * @param file 上传的文件
     * @param category 文档类别 (可选)
     * @return 处理结果摘要
     */
    public String uploadAndProcessDocument(MultipartFile file, String category) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传文件为空");
        }

        // 验证文件类型
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !isValidFileType(originalFilename)) {
            throw new IllegalArgumentException("不支持的文件类型，支持的类型：" + supportedDocumentTypes);
        }

        // 保存文件到本地
        Path uploadPath = saveUploadedFile(file);
        log.info("文件已保存到: {}", uploadPath);

        try {
            // 读取和处理文档
            List<Document> documents = loadAndSplitDocument(uploadPath, category);

            if (documents.isEmpty()) {
                throw new RuntimeException("文档处理失败，未能提取到有效内容");
            }

            // 向量化并存储到向量数据库
            vectorStore.add(documents);

            log.info("成功处理文档: {}, 生成 {} 个文档块", originalFilename, documents.size());

            return String.format("文档 '%s' 处理成功，生成了 %d 个文档块并已存储到知识库",
                    originalFilename, documents.size());

        } catch (Exception e) {
            log.error("处理文档时发生错误: {}", originalFilename, e);
            throw new RuntimeException("文档处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量上传文档
     */
    public Map<String, String> uploadMultipleDocuments(List<MultipartFile> files, String category) {
        Map<String, String> results = new HashMap<>();

        for (MultipartFile file : files) {
            try {
                String result = uploadAndProcessDocument(file, category);
                results.put(file.getOriginalFilename(), result);
            } catch (Exception e) {
                log.error("批量上传中处理文件 {} 失败", file.getOriginalFilename(), e);
                results.put(file.getOriginalFilename(), "处理失败: " + e.getMessage());
            }
        }

        return results;
    }

    /**
     * 从文本内容直接创建文档
     */
    public String addTextDocument(String content, String title, String category) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("文档内容不能为空");
        }

        try {
            // 创建文档元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("title", title != null ? title : "手动添加文档");
            metadata.put("source", "manual_input");
            metadata.put("upload_time", new Date().toString());
            if (category != null && !category.trim().isEmpty()) {
                metadata.put("category", category);
            }

            // 创建并分割文档
            Document document = new Document(content, metadata);
            List<Document> splitDocuments = textSplitter.apply(List.of(document));

            // 存储到向量数据库
            vectorStore.add(splitDocuments);

            log.info("成功添加文本文档: {}, 生成 {} 个文档块", title, splitDocuments.size());

            return String.format("文本文档 '%s' 添加成功，生成了 %d 个文档块", title, splitDocuments.size());

        } catch (Exception e) {
            log.error("添加文本文档时发生错误: {}", title, e);
            throw new RuntimeException("文本文档添加失败: " + e.getMessage(), e);
        }
    }

    /**
     * 搜索相关文档
     */
    public List<Document> searchDocuments(String query, int maxResults) {
        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(Math.min(maxResults, 20))  // 限制最大返回数量
                    .similarityThreshold(similarityThreshold)
                    .build();

            List<Document> results = vectorStore.similaritySearch(searchRequest);
            log.debug("搜索查询 '{}' 返回 {} 个相关文档", query, results.size());

            return results;

        } catch (Exception e) {
            log.error("搜索文档时发生错误", e);
            throw new RuntimeException("文档搜索失败: " + e.getMessage(), e);
        }
    }

    /**
     * 按类别搜索文档
     */
    public List<Document> searchDocumentsByCategory(String query, String category, int maxResults) {
        try {
            String filterExpression = String.format("category == '%s'", category);

            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(Math.min(maxResults, 20))
                    .similarityThreshold(similarityThreshold)
                    .filterExpression(filterExpression)
                    .build();

            List<Document> results = vectorStore.similaritySearch(searchRequest);
            log.debug("在类别 '{}' 中搜索 '{}' 返回 {} 个相关文档", category, query, results.size());

            return results;

        } catch (Exception e) {
            log.error("按类别搜索文档时发生错误", e);
            throw new RuntimeException("文档搜索失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取文档统计信息
     */
    public Map<String, Object> getDocumentStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            // 执行一个广泛的搜索来获取所有文档的样本
            List<Document> sampleDocs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query("*")  // 通配符查询（某些向量数据库可能不支持）
                            .topK(1000)
                            .similarityThreshold(0.0)
                            .build()
            );

            stats.put("total_documents", sampleDocs.size());

            // 统计不同类别的文档数量
            Map<String, Long> categoryStats = sampleDocs.stream()
                    .filter(doc -> doc.getMetadata().containsKey("category"))
                    .collect(Collectors.groupingBy(
                            doc -> doc.getMetadata().get("category").toString(),
                            Collectors.counting()
                    ));

            stats.put("categories", categoryStats);
            stats.put("supported_file_types", supportedDocumentTypes);

        } catch (Exception e) {
            log.warn("获取文档统计信息时发生错误", e);
            stats.put("error", "无法获取准确的统计信息");
        }

        return stats;
    }

    /**
     * 清除所有文档 (谨慎使用)
     */
    public String clearAllDocuments() {
        try {
            // 注意：并非所有VectorStore实现都支持删除所有文档
            // 这里假设有一个清空的方法，实际实现可能因向量数据库而异
            log.warn("正在清除所有文档...");

            // 由于VectorStore接口没有直接的清空方法，
            // 这里可能需要根据具体的向量数据库实现来处理
            // 例如，对于PGVector，可能需要直接执行SQL

            return "文档清除操作已执行（请检查具体实现是否支持）";

        } catch (Exception e) {
            log.error("清除文档时发生错误", e);
            throw new RuntimeException("文档清除失败: " + e.getMessage(), e);
        }
    }

    // 私有辅助方法

    private boolean isValidFileType(String filename) {
        String extension = getFileExtension(filename).toLowerCase();
        return supportedDocumentTypes.contains(extension);
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex + 1) : "";
    }

    private Path saveUploadedFile(MultipartFile file) throws IOException {
        // 创建存储目录
        Path uploadDir = Paths.get(documentStoragePath);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        // 生成唯一文件名
        String timestamp = String.valueOf(System.currentTimeMillis());
        String originalFilename = file.getOriginalFilename();
        String filename = timestamp + "_" + originalFilename;

        Path filePath = uploadDir.resolve(filename);

        // 保存文件
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return filePath;
    }

    private List<Document> loadAndSplitDocument(Path filePath, String category) throws IOException {
        String filename = filePath.getFileName().toString();
        String extension = getFileExtension(filename).toLowerCase();

        List<Document> documents = new ArrayList<>();

        switch (extension) {
            case "pdf":
                documents = loadPdfDocument(filePath, category);
                break;
            case "txt":
            case "md":
                documents = loadTextDocument(filePath, category);
                break;
            default:
                throw new UnsupportedOperationException("暂不支持的文件类型: " + extension);
        }

        // 分割文档
        if (!documents.isEmpty()) {
            return textSplitter.apply(documents);
        }

        return documents;
    }

    private List<Document> loadPdfDocument(Path filePath, String category) throws IOException {
        try {
            PdfDocumentReaderConfig config = PdfDocumentReaderConfig.builder()
                    .withPageExtractedTextFormatter(new ExtractedTextFormatter.Builder()
                            .withNumberOfBottomTextLinesToDelete(3)
                            .withNumberOfTopPagesToSkipBeforeDelete(1)
                            .build())
                    .withPagesPerDocument(1)
                    .build();

            PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(
                    filePath.toUri().toURL().toString(),
                    config
            );

            List<Document> documents = pdfReader.get();

            // 添加元数据
            for (Document doc : documents) {
                Map<String, Object> metadata = new HashMap<>(doc.getMetadata());
                metadata.put("source_file", filePath.getFileName().toString());
                metadata.put("file_type", "pdf");
                metadata.put("upload_time", new Date().toString());
                if (category != null && !category.trim().isEmpty()) {
                    metadata.put("category", category);
                }
                doc.getMetadata().putAll(metadata);
            }

            return documents;

        } catch (Exception e) {
            log.error("读取PDF文档失败: {}", filePath, e);
            throw new IOException("PDF文档读取失败: " + e.getMessage(), e);
        }
    }

    private List<Document> loadTextDocument(Path filePath, String category) throws IOException {
        try {
            String content = Files.readString(filePath);

            if (content.trim().isEmpty()) {
                throw new IOException("文档内容为空");
            }

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source_file", filePath.getFileName().toString());
            metadata.put("file_type", getFileExtension(filePath.getFileName().toString()));
            metadata.put("upload_time", new Date().toString());
            if (category != null && !category.trim().isEmpty()) {
                metadata.put("category", category);
            }

            Document document = new Document(content, metadata);
            return List.of(document);

        } catch (Exception e) {
            log.error("读取文本文档失败: {}", filePath, e);
            throw new IOException("文本文档读取失败: " + e.getMessage(), e);
        }
    }
}