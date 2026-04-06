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
    
    @Value("${nvidia.api.embedding-model:nvidia/nv-embed-v1}")
    private String model;
    
    /**
     * Generate embeddings for a batch of texts with comprehensive error handling.
     */
    public List<float[]> generateEmbeddings(List<String> texts) {
        String url = baseUrl + "/embeddings";
        
        log.info("[NvidiaEmbeddingClient] Calling NVIDIA API - url: {}, model: {}, batchSize: {}, apiKeyLength: {}", 
                 url, model, texts.size(), apiKey != null ? apiKey.length() : 0);
        
        try {
            // Build request
            NvidiaEmbeddingRequest request = new NvidiaEmbeddingRequest();
            request.setInput(texts);
            request.setModel(model);
            request.setInputType("query");
            request.setEncodingFormat("float");
            request.setTruncate("NONE");
            
            log.info("[NvidiaEmbeddingClient] Request details - input: {}, model: {}, inputType: {}, encodingFormat: {}, truncate: {}", 
                    texts.size(), request.getModel(), request.getInputType(), request.getEncodingFormat(), request.getTruncate());
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            
            log.debug("[NvidiaEmbeddingClient] Headers set - ContentType: {}, Authorization: Bearer ***", 
                     headers.getContentType());
            
            HttpEntity<NvidiaEmbeddingRequest> entity = new HttpEntity<>(request, headers);
            
            // Call API
            log.info("[NvidiaEmbeddingClient] Making POST request to NVIDIA API...");
            NvidiaEmbeddingResponse response = restTemplate.postForObject(url, entity, NvidiaEmbeddingResponse.class);
            
            // Validate response
            if (response == null) {
                log.error("[NvidiaEmbeddingClient] NVIDIA API returned null response");
                throw new RuntimeException("NVIDIA embedding API returned null response");
            }
            
            if (response.getData() == null || response.getData().isEmpty()) {
                log.error("[NvidiaEmbeddingClient] NVIDIA API returned empty data - response: {}", response);
                throw new RuntimeException("NVIDIA embedding API returned empty data");
            }
            
            List<float[]> embeddings = response.getData().stream()
                    .map(NvidiaEmbeddingData::getEmbedding)
                    .toList();
            
            // Validate embedding count matches input count
            if (embeddings.size() != texts.size()) {
                log.error("[NvidiaEmbeddingClient] Embedding count mismatch - expected: {}, received: {}", 
                         texts.size(), embeddings.size());
                throw new RuntimeException(String.format("Embedding count mismatch: expected %d, got %d", 
                                                        texts.size(), embeddings.size()));
            }
            
            log.info("[NvidiaEmbeddingClient] NVIDIA API call successful - embeddingsCount: {}, dimensions: {}", 
                     embeddings.size(), embeddings.get(0).length);
            
            return embeddings;
            
        } catch (org.springframework.web.client.RestClientException e) {
            log.error("[NvidiaEmbeddingClient] REST client error calling NVIDIA API - url: {}, errorType: {}, message: {}", 
                     url, e.getClass().getSimpleName(), e.getMessage(), e);
            throw new RuntimeException("Failed to call NVIDIA embedding API: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("[NvidiaEmbeddingClient] Unexpected error calling NVIDIA API - url: {}, errorType: {}, message: {}", 
                     url, e.getClass().getSimpleName(), e.getMessage(), e);
            throw new RuntimeException("Unexpected error calling NVIDIA embedding API: " + e.getMessage(), e);
        }
    }
    
    static class NvidiaEmbeddingRequest {
        private List<String> input;
        private String model;
        @JsonProperty("input_type")
        private String inputType;
        @JsonProperty("encoding_format")
        private String encodingFormat;
        private String truncate;
        
        public List<String> getInput() { return input; }
        public void setInput(List<String> input) { this.input = input; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getInputType() { return inputType; }
        public void setInputType(String inputType) { this.inputType = inputType; }
        public String getEncodingFormat() { return encodingFormat; }
        public void setEncodingFormat(String encodingFormat) { this.encodingFormat = encodingFormat; }
        public String getTruncate() { return truncate; }
        public void setTruncate(String truncate) { this.truncate = truncate; }
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
