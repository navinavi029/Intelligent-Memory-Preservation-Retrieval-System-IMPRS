-- Create database
CREATE DATABASE pdf_rag_db;

-- Connect to the database
\c pdf_rag_db

-- Enable PGVector extension
CREATE EXTENSION IF NOT EXISTS vector;
