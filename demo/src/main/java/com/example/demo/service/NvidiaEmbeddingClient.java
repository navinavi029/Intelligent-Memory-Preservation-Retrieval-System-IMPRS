package com.example.demo.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Direct REST client for NVIDIA embedding API.
 * Bypasses Spring AI's OpenAI client to avoid compatibility issues.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NvidiaEmbeddingClient {
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${nvidia.api.key}")
    private String apiKey;
    
    @Value("${nvidia.api.base-url:https://integrate.api.nvidia.com/v1}")
    private String baseUrl;
    
    @Value("${nvidia.api.embedding-model:nvidia/llama-3.2-nemoretriever-300m-embed-v1}")
    private String model;
    
    /**
     * Generate embeddings for a batch of texts.
     */
    public List<float[]> generateEmbeddings(List<String> texts) {
        String url = baseUrl + "/embeddings";
        
        log.debug("[NvidiaEmbeddingClient] Calling NVIDIA API - url: {}, model: {}, batchSize: {}", 
                 url, model, texts.size());
        
        // Build request
        NvidiaEmbeddingRequest request = new NvidiaEmbeddingRequest();
        request.setInput(texts);
        request.setModel(model);
        request.setInputType("passage");
        request.setEncodingFormat("float");
        
        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        
        HttpEntity<NvidiaEmbeddingRequest> entity = new HttpEntity<>(request, headers);
        
        // Call API
        NvidiaEmbeddingResponse response = restTemplate.postForObject(url, entity, NvidiaEmbeddingResponse.class);
        
        if (response == null || response.getData() == null) {
            throw new RuntimeException("NVIDIA API returned null response");
        }
        
        log.debug("[NvidiaEmbeddingClient] NVIDIA API call successful - embeddingsCount: {}", 
                 response.getData().size());
        
        // Extract embeddings
        return response.getData().stream()
                .map(NvidiaEmbeddingData::getEmbedding)
                .toList();
    }
    
    @Data
    static class NvidiaEmbeddingRequest {
        private List<String> input;
        private String model;
        @JsonProperty("input_type")
        private String inputType;
        @JsonProperty("encoding_format")
        private String encodingFormat;
    }
    
    @Data
    static class NvidiaEmbeddingResponse {
        private String object;
        private List<NvidiaEmbeddingData> data;
        private String model;
        private NvidiaUsage usage;
    }
    
    @Data
    static class NvidiaEmbeddingData {
        private int index;
        private float[] embedding;
        private String object;
    }
    
    @Data
    static class NvidiaUsage {
        @JsonProperty("prompt_tokens")
        private int promptTokens;
        @JsonProperty("total_tokens")
        private int totalTokens;
    }
}
