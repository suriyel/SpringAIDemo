# Spring AI Alibaba + RAG 检索增强生成系统

## 项目简介

这是一个基于Spring AI和阿里云通义模型的智能对话系统，集成了RAG（检索增强生成）功能。系统能够：

- 🤖 与阿里云通义千问模型进行多轮对话
- 📚 支持文档上传和知识库管理
- 🔍 基于向量相似性的文档检索
- 🧠 结合上下文信息生成准确回答
- 📊 提供丰富的API接口和监控功能

## 核心特性

### 🚀 多模式对话
- **普通对话模式**：基于模型训练数据的标准对话
- **RAG对话模式**：结合知识库内容的增强对话
- **智能对话模式**：自动判断是否需要检索外部知识
- **分类对话模式**：在特定文档类别中进行检索

### 📄 强大的文档处理
- 支持多种文档格式：PDF、TXT、Markdown、Word
- 智能文档分块和向量化
- 文档分类管理
- 批量文档上传
- 文档相关性分析

### 🔧 灵活的配置
- 可配置的向量相似度阈值
- 可调节的检索结果数量
- 支持查询重写优化
- 多种向量数据库支持

## 快速开始

### 1. 环境准备

#### 必需的环境变量
```bash
# 阿里云通义模型API Key
export AI_DASHSCOPE_API_KEY="your_dashscope_api_key"

# OpenAI API Key (用于文档向量化)
export OPENAI_API_KEY="your_openai_api_key"

# 数据库配置 (可选，默认使用Docker配置)
export DB_USERNAME="postgres"
export DB_PASSWORD="postgres"
```

#### 启动PostgreSQL向量数据库
```bash
# 启动基础服务
docker-compose up -d postgres

# 启动包含管理界面的完整服务
docker-compose --profile admin up -d
```

### 2. 构建和运行

```bash
# 编译项目
mvn clean compile

# 运行测试 (可选，需要网络连接)
mvn test

# 启动应用
mvn spring-boot:run
```

### 3. API接口使用

#### 3.1 普通对话

```bash
# 普通对话
curl -X POST "http://localhost:8080/api/chat" \
  -d "message=你好，请介绍一下自己" \
  -d "sessionId=test_session"
```

#### 3.2 文档管理

```bash
# 上传文档
curl -X POST "http://localhost:8080/api/rag/documents/upload" \
  -F "file=@document.pdf" \
  -F "category=技术文档"

# 添加文本文档
curl -X POST "http://localhost:8080/api/rag/documents/add-text" \
  -d "content=这是一段技术文档内容..." \
  -d "title=技术指南" \
  -d "category=技术文档"

# 搜索文档
curl -X GET "http://localhost:8080/api/rag/documents/search?query=Spring AI&maxResults=5"
```

#### 3.3 RAG对话

```bash
# RAG增强对话
curl -X POST "http://localhost:8080/api/rag/chat" \
  -d "message=Spring AI的核心特性有哪些？" \
  -d "sessionId=rag_session"

# 智能对话 (自动选择模式)
curl -X POST "http://localhost:8080/api/rag/smart-chat" \
  -d "message=请介绍一下向量数据库" \
  -d "sessionId=smart_session"

# 分类RAG对话
curl -X POST "http://localhost:8080/api/rag/chat/category" \
  -d "message=公司的远程工作政策是什么？" \
  -d "category=公司政策" \
  -d "sessionId=policy_session"
```

#### 3.4 系统监控

```bash
# 获取文档统计
curl -X GET "http://localhost:8080/api/rag/documents/stats"

# 获取RAG系统状态
curl -X GET "http://localhost:8080/api/rag/status"

# 文档相关性分析
curl -X GET "http://localhost:8080/api/rag/documents/analyze?query=人工智能"
```

## 配置说明

### application.yml 主要配置项

```yaml
app:
  rag:
    similarity-threshold: 0.75    # 向量相似度阈值 (0.0-1.0)
    top-k: 5                     # 检索返回文档数量
    chunk-size: 1000             # 文档分块大小
    chunk-overlap: 200           # 分块重叠大小
    enable-rewrite-query: true   # 是否启用查询重写
    document-storage-path: "documents/" # 文档存储路径
  
  supported-document-types:      # 支持的文档类型
    - "pdf"
    - "txt"
    - "md"
    - "docx"
```

### 向量数据库配置

```yaml
spring:
  ai:
    vectorstore:
      pgvector:
        index-type: HNSW              # 索引类型
        distance-type: COSINE_DISTANCE # 距离计算方式
        dimensions: 1536              # 向量维度
        initialize-schema: true       # 自动初始化schema
        max-document-batch-size: 1000 # 批量处理大小
```

## 架构设计

### 核心组件

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   用户请求      │───▶│   RAG Controller │───▶│   Chat Service  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                                        │
                        ┌─────────────────┐            │
                        │ Document Service│◀───────────┘
                        └─────────────────┘
                                │
                        ┌─────────────────┐
                        │  Vector Store   │
                        │   (PGVector)    │
                        └─────────────────┘
```

### RAG工作流程

```
1. 文档预处理
   ├── 文档上传
   ├── 文本提取
   ├── 文档分块
   └── 向量化存储

2. 查询处理
   ├── 用户查询
   ├── 查询向量化
   ├── 相似性搜索
   └── 上下文检索

3. 回答生成
   ├── 上下文整合
   ├── 提示词构建
   ├── 模型推理
   └── 回答返回
```

## 最佳实践

### 1. 文档管理
- **分类组织**：为文档设置清晰的类别标签
- **内容质量**：确保上传文档内容准确、完整
- **定期更新**：及时更新过时的文档内容
- **合理分块**：根据内容特点调整分块大小

### 2. 查询优化
- **明确查询**：使用具体、明确的问题描述
- **关键词策略**：包含相关的专业术语和关键词
- **上下文利用**：在多轮对话中利用上下文信息
- **分类检索**：在特定类别中进行精确检索

### 3. 性能优化
- **相似度阈值**：根据实际效果调整阈值设置
- **检索数量**：平衡检索准确性和响应速度
- **缓存策略**：为频繁查询建立缓存机制
- **批量处理**：批量上传文档提高处理效率

## 监控和运维

### 数据库管理
访问 pgAdmin: http://localhost:8080 (如果启用了admin profile)
- 用户名: admin@example.com
- 密码: admin

### 关键监控指标
- 文档总数和分类分布
- 向量检索响应时间
- 对话会话活跃度
- 系统资源使用情况

### 日志配置
```yaml
logging:
  level:
    com.example: DEBUG
    org.springframework.ai: DEBUG
    org.springframework.ai.vectorstore: DEBUG
```

## 故障排除

### 常见问题

1. **文档上传失败**
    - 检查文件格式是否支持
    - 确认文件大小不超过限制
    - 验证磁盘空间是否充足

2. **向量检索无结果**
    - 降低相似度阈值
    - 检查文档是否成功向量化
    - 尝试不同的查询关键词

3. **模型调用超时**
    - 检查网络连接
    - 验证API Key有效性
    - 调整超时时间设置

4. **数据库连接失败**
    - 确认PostgreSQL服务运行状态
    - 检查连接参数配置
    - 验证pgvector扩展安装

### 性能调优

1. **向量数据库优化**
   ```sql
   -- 调整HNSW索引参数
   CREATE INDEX CONCURRENTLY vector_store_embedding_hnsw_idx 
   ON vector_store USING hnsw (embedding vector_cosine_ops) 
   WITH (m = 16, ef_construction = 64);
   ```

2. **应用层缓存**
    - 启用Spring Cache
    - 缓存频繁查询结果
    - 实施会话级缓存

3. **数据库连接池**
   ```yaml
   spring:
     datasource:
       hikari:
         maximum-pool-size: 10
         minimum-idle: 5
         connection-timeout: 20000
   ```

## 扩展功能

### 支持更多向量数据库
- Pinecone
- Weaviate
- ChromaDB
- Elasticsearch

### 增强的文档处理
- 图片文档OCR识别
- 音频文档转录
- 网页内容爬取
- 实时文档同步

### 高级RAG技术
- 混合检索 (关键词 + 语义)
- 文档重排序
- 多跳推理
- 对话历史利用

## 贡献指南

欢迎提交Issue和Pull Request来完善这个项目！

### 开发环境设置
1. Fork项目到个人仓库
2. 克隆到本地开发环境
3. 创建功能分支
4. 提交代码并创建PR

### 代码规范
- 遵循Java编码规范
- 添加必要的单元测试
- 更新相关文档
- 确保所有测试通过

## 许可证

MIT License

## 联系方式

如有问题或建议，请通过以下方式联系：
- 提交GitHub Issue
- 发送邮件到: [your-email@example.com]
- 加入讨论群: [群号或链接]