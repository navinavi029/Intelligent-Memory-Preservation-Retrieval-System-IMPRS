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
 * Direct REST client for NVIDIA chat completion API.
 * Bypasses Spring AI's OpenAI client to avoid compatibility issues.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NvidiaChatClient {
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${nvidia.api.key}")
    private String apiKey;
    
    @Value("${nvidia.api.base-url:https://integrate.api.nvidia.com/v1}")
    private String baseUrl;
    
    @Value("${nvidia.api.chat-model:meta/llama-3.1-8b-instruct}")
    private String model;
    
    @Value("${nvidia.api.max-tokens:500}")
    private int maxTokens;
    
    /**
     * Generate chat completion response.
     */
    public String generateResponse(String systemPrompt, String userMessage) {
        String url = baseUrl + "/chat/completions";
        
        log.debug("[NvidiaChatClient] Calling NVIDIA chat API - url: {}, model: {}", url, model);
        
        // Build request
        NvidiaChatRequest request = new NvidiaChatRequest();
        request.setModel(model);
        request.setMaxTokens(maxTokens);
        request.setTemperature(0.7);
        request.setTopP(1.0);
        request.setStream(false);
        
        // Add messages
        request.setMessages(List.of(
            new ChatMessage("system", systemPrompt),
            new ChatMessage("user", userMessage)
        ));
        
        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        
        HttpEntity<NvidiaChatRequest> entity = new HttpEntity<>(request, headers);
        
        // Call API
        NvidiaChatResponse response = restTemplate.postForObject(url, entity, NvidiaChatResponse.class);
        
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            throw new RuntimeException("NVIDIA chat API returned null or empty response");
        }
        
        String content = response.getChoices().get(0).getMessage().getContent();
        
        log.debug("[NvidiaChatClient] NVIDIA chat API call successful - responseLength: {}", content.length());
        
        return content;
    }
    
    @Data
    static class NvidiaChatRequest {
        private String model;
        private List<ChatMessage> messages;
        @JsonProperty("max_tokens")
        private Integer maxTokens;
        private Double temperature;
        @JsonProperty("top_p")
        private Double topP;
        private Boolean stream;
    }
    
    @Data
    static class ChatMessage {
        private String role;
        private String content;
        
        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
    
    @Data
    static class NvidiaChatResponse {
        private String id;
        private String object;
        private Long created;
        private String model;
        private List<Choice> choices;
        private Usage usage;
    }
    
    @Data
    static class Choice {
        private Integer index;
        private ChatMessage message;
        @JsonProperty("finish_reason")
        private String finishReason;
    }
    
    @Data
    static class Usage {
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;
        @JsonProperty("completion_tokens")
        private Integer completionTokens;
        @JsonProperty("total_tokens")
        private Integer totalTokens;
    }
}
