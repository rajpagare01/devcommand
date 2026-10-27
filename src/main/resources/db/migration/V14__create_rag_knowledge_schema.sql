CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS knowledge_documents (
    id BIGSERIAL PRIMARY KEY,
    document_id VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    source VARCHAR(255),
    owner_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    visibility VARCHAR(30) NOT NULL,
    tags VARCHAR(255),
    checksum VARCHAR(64) NOT NULL,
    chunk_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_knowledge_docs_owner_doc ON knowledge_documents(owner_id, document_id) WHERE owner_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_knowledge_docs_system_doc ON knowledge_documents(document_id) WHERE owner_id IS NULL;
CREATE INDEX IF NOT EXISTS idx_knowledge_docs_visibility ON knowledge_documents(visibility);
CREATE INDEX IF NOT EXISTS idx_knowledge_docs_document_type ON knowledge_documents(document_type);
CREATE INDEX IF NOT EXISTS idx_knowledge_docs_owner_id ON knowledge_documents(owner_id);

CREATE TABLE IF NOT EXISTS vector_store (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    content TEXT,
    metadata JSONB,
    embedding vector(1536)
);

CREATE INDEX IF NOT EXISTS vector_store_embedding_idx ON vector_store USING HNSW (embedding vector_cosine_ops);
