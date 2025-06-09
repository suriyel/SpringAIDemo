-- 数据库初始化脚本
-- 为AI知识库创建必要的扩展和表结构

-- 启用必要的扩展
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 创建向量存储表
CREATE TABLE IF NOT EXISTS vector_store (
    id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    content text NOT NULL,
    metadata json,
    embedding vector(1536), -- OpenAI text-embedding-3-small 的向量维度
    created_at timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp DEFAULT CURRENT_TIMESTAMP
);

-- 创建HNSW索引以提高相似性搜索性能
CREATE INDEX IF NOT EXISTS vector_store_embedding_idx
ON vector_store USING hnsw (embedding vector_cosine_ops);

-- 创建元数据索引
CREATE INDEX IF NOT EXISTS vector_store_metadata_idx
ON vector_store USING gin (metadata);

-- 创建文档管理表（可选，用于跟踪原始文档）
CREATE TABLE IF NOT EXISTS documents (
    id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    filename varchar(255) NOT NULL,
    original_filename varchar(255) NOT NULL,
    file_type varchar(50) NOT NULL,
    file_size bigint,
    category varchar(100),
    upload_time timestamp DEFAULT CURRENT_TIMESTAMP,
    processed boolean DEFAULT false,
    chunk_count integer DEFAULT 0,
    metadata json
);

-- 创建文档索引
CREATE INDEX IF NOT EXISTS documents_category_idx ON documents(category);
CREATE INDEX IF NOT EXISTS documents_file_type_idx ON documents(file_type);
CREATE INDEX IF NOT EXISTS documents_upload_time_idx ON documents(upload_time);

-- 创建会话管理表（可选，用于跟踪对话会话）
CREATE TABLE IF NOT EXISTS chat_sessions (
    id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    session_id varchar(255) UNIQUE NOT NULL,
    created_at timestamp DEFAULT CURRENT_TIMESTAMP,
    last_active timestamp DEFAULT CURRENT_TIMESTAMP,
    message_count integer DEFAULT 0,
    metadata json
);

-- 创建会话索引
CREATE INDEX IF NOT EXISTS chat_sessions_session_id_idx ON chat_sessions(session_id);
CREATE INDEX IF NOT EXISTS chat_sessions_last_active_idx ON chat_sessions(last_active);

-- 创建更新时间戳的函数
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 为vector_store表创建更新触发器
CREATE TRIGGER update_vector_store_updated_at
    BEFORE UPDATE ON vector_store
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- 创建一些实用的视图

-- 文档统计视图
CREATE OR REPLACE VIEW document_stats AS
SELECT
    COUNT(*) as total_documents,
    COUNT(CASE WHEN processed = true THEN 1 END) as processed_documents,
    COUNT(CASE WHEN processed = false THEN 1 END) as pending_documents,
    COUNT(DISTINCT category) as unique_categories,
    COUNT(DISTINCT file_type) as unique_file_types,
    SUM(chunk_count) as total_chunks,
    AVG(chunk_count) as avg_chunks_per_document
FROM documents;

-- 分类统计视图
CREATE OR REPLACE VIEW category_stats AS
SELECT
    COALESCE(category, 'Uncategorized') as category,
    COUNT(*) as document_count,
    SUM(chunk_count) as total_chunks,
    AVG(chunk_count) as avg_chunks
FROM documents
GROUP BY category
ORDER BY document_count DESC;

-- 向量存储统计视图
CREATE OR REPLACE VIEW vector_store_stats AS
SELECT
    COUNT(*) as total_vectors,
    COUNT(CASE WHEN metadata->>'category' IS NOT NULL THEN 1 END) as categorized_vectors,
    COUNT(DISTINCT metadata->>'source_file') as unique_source_files,
    AVG(length(content)) as avg_content_length
FROM vector_store;

-- 插入一些示例数据（可选）
-- 您可以根据需要取消注释以下行

-- INSERT INTO documents (filename, original_filename, file_type, category, processed, chunk_count) VALUES
-- ('sample_doc_1.pdf', 'Spring AI Guide.pdf', 'pdf', 'technical', true, 15),
-- ('sample_doc_2.txt', 'Company Policy.txt', 'txt', 'policy', true, 8),
-- ('sample_doc_3.md', 'API Documentation.md', 'md', 'technical', true, 12);

-- 创建数据库函数用于向量相似性搜索（示例）
CREATE OR REPLACE FUNCTION similarity_search(
    query_embedding vector(1536),
    similarity_threshold double precision DEFAULT 0.75,
    max_results integer DEFAULT 10
)
RETURNS TABLE(
    id uuid,
    content text,
    metadata json,
    similarity double precision
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        vs.id,
        vs.content,
        vs.metadata,
        1 - (vs.embedding <=> query_embedding) as similarity
    FROM vector_store vs
    WHERE 1 - (vs.embedding <=> query_embedding) >= similarity_threshold
    ORDER BY vs.embedding <=> query_embedding
    LIMIT max_results;
END;
$$ LANGUAGE plpgsql;

-- 授予必要的权限（根据需要调整）
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO your_app_user;
-- GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO your_app_user;

-- 显示初始化完成信息
DO $$
BEGIN
    RAISE NOTICE 'AI Knowledge Database initialization completed successfully!';
    RAISE NOTICE 'Available tables: vector_store, documents, chat_sessions';
    RAISE NOTICE 'Available views: document_stats, category_stats, vector_store_stats';
    RAISE NOTICE 'Vector extension enabled with HNSW indexing';
END $$;