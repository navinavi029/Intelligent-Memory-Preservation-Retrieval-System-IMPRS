-- Database Initialization Script for PDF RAG Chatbot
-- This script creates the necessary database schema with PGVector extension

-- Enable PGVector extension for vector similarity search
CREATE EXTENSION IF NOT EXISTS vector;

-- Documents table
-- Stores metadata about uploaded PDF documents
CREATE TABLE IF NOT EXISTS documents (
    id BIGSERIAL PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    file_size BIGINT,
    upload_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL,
    chunk_count INTEGER,
    error_message TEXT
);

-- Document chunks table with vector embeddings
-- Stores text chunks and their vector embeddings for semantic search
-- Note: Using vector(2000) dimensions due to pgvector index limits
-- Embeddings from nvidia/nv-embed-v1 (4096 dims) are truncated to 2000 dimensions
CREATE TABLE IF NOT EXISTS document_chunks (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    chunk_number INTEGER NOT NULL,
    content TEXT NOT NULL,
    token_count INTEGER,
    embedding vector(2000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_document_chunk UNIQUE (document_id, chunk_number)
);

-- HNSW index for efficient vector similarity search using cosine distance
-- HNSW provides excellent performance for high-dimensional vectors
-- m parameter controls the number of connections (16 is a good default)
-- ef_construction controls index build quality (64 is a good default)
CREATE INDEX IF NOT EXISTS idx_chunks_embedding ON document_chunks 
USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64);

-- Index for filtering documents by processing status
CREATE INDEX IF NOT EXISTS idx_documents_status ON documents(status);

-- Index for efficient chunk lookups by document
CREATE INDEX IF NOT EXISTS idx_chunks_document_id ON document_chunks(document_id);
