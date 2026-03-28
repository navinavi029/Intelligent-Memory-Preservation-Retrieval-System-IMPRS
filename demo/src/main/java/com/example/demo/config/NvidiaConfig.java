package com.example.demo.config;

import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for NVIDIA NIM API integration.
 * 
 * Spring AI auto-configuration handles both chat and embedding models using the OpenAI-compatible API.
 * 
 * Properties used:
 * - spring.ai.openai.api-key: NVIDIA API key for authentication
 * - spring.ai.openai.base-url: https://integrate.api.nvidia.com/v1
 * - spring.ai.openai.chat.options.model: meta/llama-3.1-8b-instruct
 * - spring.ai.openai.chat.options.max-tokens: 500
 * - spring.ai.openai.embedding.options.model: nvidia/llama-3.2-nemoretriever-300m-embed-v1
 */
@Configuration
public class NvidiaConfig {
    // Configuration is handled by Spring AI auto-configuration
}
