# Spring AI RAG API 使用示例

## 目录
1. [基础对话API](#基础对话api)
2. [文档管理API](#文档管理api)
3. [RAG对话API](#rag对话api)
4. [系统监控API](#系统监控api)
5. [高级功能API](#高级功能api)
6. [错误处理示例](#错误处理示例)

## 基础对话API

### 1. 普通对话
```bash
# 简单对话
curl -X POST "http://localhost:8080/api/chat" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "message=你好，请介绍一下你自己"

# 指定会话ID的对话
curl -X POST "http://localhost:8080/api/chat" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "message=我想了解Spring框架" \
  -d "sessionId=user123_session"
```

**响应示例：**
```json
{
  "success": true,
  "response": "你好！我是一个基于阿里云通义千问模型的智能助手...",
  "sessionId": "user123_session",
  "processingTime": "1200ms"
}
```

### 2. 获取对话历史
```bash
curl -X GET "http://localhost:8080/api/rag/chat/history?sessionId=user123_session"
```

### 3. 重置对话会话
```bash
curl -X POST "http://localhost:8080/api/rag/chat/reset?sessionId=user123_session"
```

## 文档管理API

### 1. 单文档上传
```bash
# 上传PDF文档
curl -X POST "http://localhost:8080/api/rag/documents/upload" \
  -F "file=@./documents/spring_ai_guide.pdf" \
  -F "category=技术文档"

# 上传文本文档
curl -X POST "http://localhost:8080/api/rag/documents/upload" \
  -F "file=@./documents/company_policy.txt" \
  -F "category=公司政策"
```

**响应示例：**
```json
{
  "success": true,
  "message": "文档 'spring_ai_guide.pdf' 处理成功，生成了 15 个文档块并已存储到知识库",
  "filename": "spring_ai_guide.pdf",
  "category": "技术文档"
}
```

### 2. 批量文档上传
```bash
curl -X POST "http://localhost:8080/api/rag/documents/upload-batch" \
  -F "files=@./documents/doc1.pdf" \
  -F "files=@./documents/doc2.txt" \
  -F "files=@./documents/doc3.md" \
  -F "category=技术文档"
```

**响应示例：**
```json
{
  "success": true,
  "results": {
    "doc1.pdf": "文档 'doc1.pdf' 处理成功，生成了 12 个文档块",
    "doc2.txt": "文档 'doc2.txt' 处理成功，生成了 8 个文档块", 
    "doc3.md": "文档 'doc3.md' 处理成功，生成了 10 个文档块"
  },
  "totalFiles": 3,
  "successCount": 3,
  "failureCount": 0,
  "category": "技术文档"
}
```

### 3. 添加文本文档
```bash
curl -X POST "http://localhost:8080/api/rag/documents/add-text" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "content=Spring AI是一个强大的人工智能应用框架，它为Java开发者提供了与各种AI模型集成的能力。主要特性包括：1. 统一的API接口 2. 多模型支持 3. 向量存储集成 4. RAG功能支持" \
  -d "title=Spring AI概述" \
  -d "category=技术文档"
```

### 4. 文档搜索
```bash
# 基础搜索
curl -X GET "http://localhost:8080/api/rag/documents/search?query=Spring%20AI特性&maxResults=5"

# 按类别搜索
curl -X GET "http://localhost:8080/api/rag/documents/search/category?query=远程工作&category=公司政策&maxResults=3"
```

**搜索响应示例：**
```json
{
  "success": true,
  "query": "Spring AI特性",
  "totalResults": 3,
  "documents": [
    {
      "content": "Spring AI是一个强大的人工智能应用框架...",
      "summary": "Spring AI是一个强大的人工智能应用框架，它为Java开发者提供了与各种AI模型集成的能力。主要特性包括：1. 统一的API接口 2. 多模型支持...",
      "metadata": {
        "title": "Spring AI概述",
        "category": "技术文档",
        "source": "manual_input",
        "upload_time": "2025-06-10T10:30:00"
      }
    }
  ]
}
```

## RAG对话API

### 1. 基础RAG对话
```bash
curl -X POST "http://localhost:8080/api/rag/chat" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "message=Spring AI支持哪些向量数据库？" \
  -d "sessionId=rag_session_001"
```

**响应示例：**
```json
{
  "success": true,
  "response": "根据Spring AI文档，Spring AI支持多种向量数据库，包括：1. PGVector (PostgreSQL扩展) 2. Pinecone 3. Weaviate 4. ChromaDB 5. Elasticsearch 6. Redis等。每种向量数据库都有其特定的优势和使用场景...",
  "sessionId": "rag_session_001", 
  "processingTime": "2100ms",
  "mode": "RAG"
}
```

### 2. 智能对话模式
```bash
# 智能模式会自动判断是否需要使用RAG
curl -X POST "http://localhost:8080/api/rag/smart-chat" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "message=向量数据库在AI应用中的作用是什么？" \
  -d "sessionId=smart_session_001"
```

### 3. 分类RAG对话
```bash
# 在特定文档类别中进行RAG对话
curl -X POST "http://localhost:8080/api/rag/chat/category" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "message=员工请假需要提前多久申请？" \
  -d "category=公司政策" \
  -d "sessionId=policy_session_001"
```

**分类RAG响应示例：**
```json
{
  "success": true,
  "response": "根据公司政策文档，员工请假需要提前1周申请。具体规定如下：1. 年假需要提前2周申请 2. 病假可以事后补办手续 3. 紧急事假需要在当天内申请...",
  "sessionId": "policy_session_001",
  "category": "公司政策", 
  "processingTime": "1800ms",
  "mode": "RAG_CATEGORY"
}
```

## 系统监控API

### 1. 获取文档统计信息
```bash
curl -X GET "http://localhost:8080/api/rag/documents/stats"
```

**统计响应示例：**
```json
{
  "success": true,
  "total_documents": 25,
  "categories": {
    "技术文档": 15,
    "公司政策": 8,
    "产品说明": 2
  },
  "supported_file_types": ["pdf", "txt", "md", "docx"]
}
```

### 2. 获取RAG系统状态
```bash
curl -X GET "http://localhost:8080/api/rag/status"
```

**状态响应示例：**
```json
{
  "success": true,
  "status": "=== RAG系统状态 ===\n文档总数: 25\n支持的文件类型: [pdf, txt, md, docx]\n文档类别分布:\n  - 技术文档: 15 个文档\n  - 公司政策: 8 个文档\n  - 产品说明: 2 个文档\nRAG功能: 已启用\n向量存储: PGVector\n嵌入模型: OpenAI text-embedding-3-small",
  "timestamp": 1717999800000
}
```

### 3. 文档相关性分析
```bash
curl -X GET "http://localhost:8080/api/rag/documents/analyze?query=人工智能和机器学习"
```

**相关性分析响应示例：**
```json
{
  "success": true,
  "query": "人工智能和机器学习",
  "analysis": "找到 3 个相关文档：\n\n1. 来源: ai_intro.pdf | 类别: 技术文档\n   摘要: 人工智能(AI)是计算机科学的一个分支，旨在创建能够执行通常需要人类智能的任务的系统...\n\n2. 来源: ml_guide.txt | 类别: 技术文档\n   摘要: 机器学习是人工智能的一个子集，它使计算机能够在没有明确编程的情况下学习和做出决策..."
}
```

## 高级功能API

### 1. 多轮RAG对话示例
```bash
# 第一轮：建立上下文
curl -X POST "http://localhost:8080/api/rag/chat" \
  -d "message=请介绍一下Spring AI的向量存储功能" \
  -d "sessionId=advanced_session"

# 第二轮：深入询问
curl -X POST "http://localhost:8080/api/rag/chat" \
  -d "message=这些向量存储如何配置？" \
  -d "sessionId=advanced_session"

# 第三轮：应用场景
curl -X POST "http://localhost:8080/api/rag/chat" \
  -d "message=在实际项目中如何选择合适的向量数据库？" \
  -d "sessionId=advanced_session"
```

### 2. 复杂查询示例
```bash
# 复合查询
curl -X POST "http://localhost:8080/api/rag/smart-chat" \
  -d "message=比较Spring AI和LangChain在向量存储集成方面的优缺点" \
  -d "sessionId=comparison_session"

# 技术细节查询
curl -X POST "http://localhost:8080/api/rag/chat/category" \
  -d "message=如何在Spring Boot应用中配置PGVector作为向量存储？需要哪些依赖和配置?" \
  -d "category=技术文档" \
  -d "sessionId=technical_session"
```

### 3. 批量操作示例
```bash
# 批量文档处理脚本示例
#!/bin/bash

# 定义文档目录和类别
DOCS_DIR="./knowledge_base"
CATEGORIES=("技术文档" "公司政策" "产品说明" "培训资料")

# 批量上传不同类别的文档
for category in "${CATEGORIES[@]}"; do
    echo "正在上传 $category 类别的文档..."
    
    find "$DOCS_DIR/$category" -type f \( -name "*.pdf" -o -name "*.txt" -o -name "*.md" \) | while read file; do
        echo "上传文档: $file"
        curl -X POST "http://localhost:8080/api/rag/documents/upload" \
          -F "file=@$file" \
          -F "category=$category" \
          --silent --output /dev/null
        
        echo "✓ 已上传: $(basename "$file")"
        sleep 1  # 避免请求过于频繁
    done
done

echo "批量上传完成！"
```

## 错误处理示例

### 1. 文档上传错误
```bash
# 上传不支持的文件格式
curl -X POST "http://localhost:8080/api/rag/documents/upload" \
  -F "file=@./test.xlsx" \
  -F "category=测试"
```

**错误响应：**
```json
{
  "success": false,
  "error": "文档上传失败: 不支持的文件类型，支持的类型：[pdf, txt, md, docx]",
  "timestamp": 1717999800000
}
```

### 2. RAG查询错误
```bash
# 查询不存在的类别
curl -X POST "http://localhost:8080/api/rag/chat/category" \
  -d "message=测试问题" \
  -d "category=不存在的类别" \
  -d "sessionId=error_test"
```

**错误响应：**
```json
{
  "success": true,
  "response": "在类别 '不存在的类别' 中没有找到与您的问题相关的文档。请尝试调整问题或选择其他类别。",
  "sessionId": "error_test",
  "category": "不存在的类别",
  "processingTime": "500ms",
  "mode": "RAG_CATEGORY"
}
```

### 3. 系统错误处理
```bash
# 当向量数据库不可用时
curl -X GET "http://localhost:8080/api/rag/documents/stats"
```

**错误响应：**
```json
{
  "success": false,
  "error": "获取文档统计信息失败: 数据库连接超时",
  "timestamp": 1717999800000
}
```

## 客户端集成示例

### JavaScript前端集成
```javascript
class RAGClient {
    constructor(baseUrl = 'http://localhost:8080') {
        this.baseUrl = baseUrl;
        this.sessionId = this.generateSessionId();
    }

    generateSessionId() {
        return 'session_' + Math.random().toString(36).substr(2, 9);
    }

    async chat(message, useRAG = false) {
        const endpoint = useRAG ? '/api/rag/chat' : '/api/chat';
        const formData = new FormData();
        formData.append('message', message);
        formData.append('sessionId', this.sessionId);

        try {
            const response = await fetch(this.baseUrl + endpoint, {
                method: 'POST',
                body: formData
            });
            return await response.json();
        } catch (error) {
            console.error('Chat error:', error);
            throw error;
        }
    }

    async smartChat(message) {
        const formData = new FormData();
        formData.append('message', message);
        formData.append('sessionId', this.sessionId);

        try {
            const response = await fetch(this.baseUrl + '/api/rag/smart-chat', {
                method: 'POST',
                body: formData
            });
            return await response.json();
        } catch (error) {
            console.error('Smart chat error:', error);
            throw error;
        }
    }

    async uploadDocument(file, category = '') {
        const formData = new FormData();
        formData.append('file', file);
        if (category) formData.append('category', category);

        try {
            const response = await fetch(this.baseUrl + '/api/rag/documents/upload', {
                method: 'POST',
                body: formData
            });
            return await response.json();
        } catch (error) {
            console.error('Upload error:', error);
            throw error;
        }
    }

    async getSystemStatus() {
        try {
            const response = await fetch(this.baseUrl + '/api/rag/status');
            return await response.json();
        } catch (error) {
            console.error('Status error:', error);
            throw error;
        }
    }
}

// 使用示例
const ragClient = new RAGClient();

// 智能对话
ragClient.smartChat('Spring AI的主要优势是什么？')
    .then(response => console.log('AI回复:', response.response))
    .catch(error => console.error('错误:', error));

// 文档上传
const fileInput = document.getElementById('fileInput');
fileInput.addEventListener('change', async (event) => {
    const file = event.target.files[0];
    if (file) {
        try {
            const result = await ragClient.uploadDocument(file, '技术文档');
            console.log('上传成功:', result.message);
        } catch (error) {
            console.error('上传失败:', error);
        }
    }
});
```

### Python客户端集成
```python
import requests
import json
from typing import Optional, Dict, Any

class RAGClient:
    def __init__(self, base_url: str = "http://localhost:8080"):
        self.base_url = base_url
        self.session_id = f"python_session_{hash(id(self))}"

    def chat(self, message: str, use_rag: bool = False) -> Dict[str, Any]:
        """发送聊天消息"""
        endpoint = "/api/rag/chat" if use_rag else "/api/chat"
        
        data = {
            'message': message,
            'sessionId': self.session_id
        }
        
        response = requests.post(f"{self.base_url}{endpoint}", data=data)
        response.raise_for_status()
        return response.json()

    def smart_chat(self, message: str) -> Dict[str, Any]:
        """智能对话（自动选择模式）"""
        data = {
            'message': message,
            'sessionId': self.session_id
        }
        
        response = requests.post(f"{self.base_url}/api/rag/smart-chat", data=data)
        response.raise_for_status()
        return response.json()

    def upload_document(self, file_path: str, category: Optional[str] = None) -> Dict[str, Any]:
        """上传文档"""
        with open(file_path, 'rb') as file:
            files = {'file': file}
            data = {}
            if category:
                data['category'] = category
                
            response = requests.post(f"{self.base_url}/api/rag/documents/upload", 
                                   files=files, data=data)
            response.raise_for_status()
            return response.json()

    def search_documents(self, query: str, max_results: int = 10) -> Dict[str, Any]:
        """搜索文档"""
        params = {
            'query': query,
            'maxResults': max_results
        }
        
        response = requests.get(f"{self.base_url}/api/rag/documents/search", params=params)
        response.raise_for_status()
        return response.json()

    def get_system_status(self) -> Dict[str, Any]:
        """获取系统状态"""
        response = requests.get(f"{self.base_url}/api/rag/status")
        response.raise_for_status()
        return response.json()

# 使用示例
if __name__ == "__main__":
    client = RAGClient()
    
    # 智能对话
    response = client.smart_chat("Spring AI支持哪些功能？")
    print("AI回复:", response['response'])
    
    # 上传文档
    try:
        upload_result = client.upload_document("./document.pdf", "技术文档")
        print("上传成功:", upload_result['message'])
    except Exception as e:
        print("上传失败:", str(e))
    
    # 搜索文档
    search_result = client.search_documents("向量数据库")
    print(f"找到 {search_result['totalResults']} 个相关文档")
    
    # 获取系统状态
    status = client.get_system_status()
    print("系统状态:", status['status'])
```

## 性能优化建议

### 1. 批量操作优化
```bash
# 使用批量上传而不是单个文档上传
curl -X POST "http://localhost:8080/api/rag/documents/upload-batch" \
  -F "files=@doc1.pdf" \
  -F "files=@doc2.pdf" \
  -F "files=@doc3.pdf" \
  -F "category=技术文档"
```

### 2. 缓存策略
```bash
# 对频繁查询的结果进行缓存
# 可以在客户端实现缓存逻辑
cache_key="query_${query_hash}"
cached_result=$(redis-cli GET $cache_key)

if [ -z "$cached_result" ]; then
    result=$(curl -X POST "http://localhost:8080/api/rag/chat" -d "message=$query")
    redis-cli SETEX $cache_key 3600 "$result"  # 缓存1小时
    echo "$result"
else
    echo "$cached_result"
fi
```

### 3. 并发控制
```bash
# 使用GNU parallel进行并发上传
find ./documents -name "*.pdf" | parallel -j 5 \
  'curl -X POST "http://localhost:8080/api/rag/documents/upload" \
   -F "file=@{}" -F "category=批量文档"'
```

这些示例涵盖了Spring AI RAG系统的主要功能和使用场景，可以帮助开发者快速集成和使用这个强大的AI对话系统。