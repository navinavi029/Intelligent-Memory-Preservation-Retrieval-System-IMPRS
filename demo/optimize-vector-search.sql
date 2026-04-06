-- Optimization script for faster vector similarity search
-- Run this after the initial schema setup to improve query performance

-- Drop the old index if it exists
DROP INDEX IF EXISTS idx_chunks_embedding;

-- Recreate HNSW index with optimized parameters for faster search
-- m=32: More connections for better recall and speed
-- ef_construction=128: Higher quality index build
CREATE INDEX idx_chunks_embedding ON document_chunks 
USING hnsw (embedding vector_cosine_ops) WITH (m = 32, ef_construction = 128);

-- Set runtime search parameter for faster queries
-- ef_search controls the search quality/speed tradeoff at query time
-- Higher values = better recall but slower
-- Lower values = faster but may miss some results
-- 40 is a good balance for speed while maintaining quality
SET hnsw.ef_search = 40;

-- To make this permanent for all sessions, you can set it in postgresql.conf:
-- hnsw.ef_search = 40

-- Analyze the table to update statistics for better query planning
ANALYZE document_chunks;

-- Verify the index exists
SELECT 
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE tablename = 'document_chunks' AND indexname = 'idx_chunks_embedding';
