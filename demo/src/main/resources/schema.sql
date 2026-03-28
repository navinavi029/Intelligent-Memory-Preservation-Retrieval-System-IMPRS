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
-- Note: Using vector(2048) dimensions. HNSW index supports max 2000 dimensions,
-- so we use IVFFlat index instead for better compatibility.
CREATE TABLE IF NOT EXISTS document_chunks (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    chunk_number INTEGER NOT NULL,
    content TEXT NOT NULL,
    token_count INTEGER,
    embedding vector(2048),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_document_chunk UNIQUE (document_id, chunk_number)
);

-- IVFFlat index for efficient vector similarity search using cosine distance
-- Using IVFFlat instead of HNSW because HNSW only supports up to 2000 dimensions
-- IVFFlat provides good performance for 2048-dimensional vectors
-- lists parameter is set to sqrt(total_rows), will be optimal when you have ~10000 chunks
CREATE INDEX IF NOT EXISTS idx_chunks_embedding ON document_chunks 
USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- Index for filtering documents by processing status
CREATE INDEX IF NOT EXISTS idx_documents_status ON documents(status);

-- Index for efficient chunk lookups by document
CREATE INDEX IF NOT EXISTS idx_chunks_document_id ON document_chunks(document_id);
