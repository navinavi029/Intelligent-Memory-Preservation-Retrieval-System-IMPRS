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
    
    @Value("${nvidia.api.chat-model:meta/llama-3.1-70b-instruct}")
    private String model;
    
    @Value("${nvidia.api.max-tokens:1024}")
    private int maxTokens;
    
    @Value("${nvidia.api.temperature:0.7}")
    private double temperature;
    
    @Value("${nvidia.api.top-p:0.9}")
    private double topP;
    
    @Value("${nvidia.api.frequency-penalty:0.1}")
    private double frequencyPenalty;
    
    @Value("${nvidia.api.presence-penalty:0.1}")
    private double presencePenalty;
    
    /**
     * Generate chat completion response with comprehensive error handling.
     */
    public String generateResponse(String systemPrompt, String userMessage) {
        String url = baseUrl + "/chat/completions";
        
        log.debug("[NvidiaChatClient] Calling NVIDIA chat API - url: {}, model: {}", url, model);
        
        try {
            // Build request with optimized parameters
            NvidiaChatRequest request = new NvidiaChatRequest();
            request.setModel(model);
            request.setMaxTokens(maxTokens);
            request.setTemperature(temperature);
            request.setTopP(topP);
            request.setFrequencyPenalty(frequencyPenalty);
            request.setPresencePenalty(presencePenalty);
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
            
            // Call API with error handling
            NvidiaChatResponse response = restTemplate.postForObject(url, entity, NvidiaChatResponse.class);
            
            // Validate response
            if (response == null) {
                log.error("[NvidiaChatClient] NVIDIA API returned null response");
                throw new RuntimeException("NVIDIA chat API returned null response");
            }
            
            if (response.getChoices() == null || response.getChoices().isEmpty()) {
                log.error("[NvidiaChatClient] NVIDIA API returned empty choices - response: {}", response);
                throw new RuntimeException("NVIDIA chat API returned empty choices");
            }
            
            ChatMessage responseMessage = response.getChoices().get(0).getMessage();
            if (responseMessage == null || responseMessage.getContent() == null) {
                log.error("[NvidiaChatClient] NVIDIA API returned null message content - choice: {}", response.getChoices().get(0));
                throw new RuntimeException("NVIDIA chat API returned null message content");
            }
            
            String content = responseMessage.getContent();
            log.debug("[NvidiaChatClient] NVIDIA chat API call successful - responseLength: {}", content.length());
            
            return content;
            
        } catch (org.springframework.web.client.RestClientException e) {
            log.error("[NvidiaChatClient] REST client error calling NVIDIA API - url: {}, error: {}", url, e.getMessage(), e);
            throw new RuntimeException("Failed to call NVIDIA chat API: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("[NvidiaChatClient] Unexpected error calling NVIDIA API - url: {}, error: {}", url, e.getMessage(), e);
            throw new RuntimeException("Unexpected error calling NVIDIA chat API: " + e.getMessage(), e);
        }
    }
    
    static class NvidiaChatRequest {
        private String model;
        private List<ChatMessage> messages;
        @JsonProperty("max_tokens")
        private Integer maxTokens;
        private Double temperature;
        @JsonProperty("top_p")
        private Double topP;
        @JsonProperty("frequency_penalty")
        private Double frequencyPenalty;
        @JsonProperty("presence_penalty")
        private Double presencePenalty;
        private Boolean stream;
        
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public List<ChatMessage> getMessages() { return messages; }
        public void setMessages(List<ChatMessage> messages) { this.messages = messages; }
        public Integer getMaxTokens() { return maxTokens; }
        public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
        public Double getTemperature() { return temperature; }
        public void setTemperature(Double temperature) { this.temperature = temperature; }
        public Double getTopP() { return topP; }
        public void setTopP(Double topP) { this.topP = topP; }
        public Double getFrequencyPenalty() { return frequencyPenalty; }
        public void setFrequencyPenalty(Double frequencyPenalty) { this.frequencyPenalty = frequencyPenalty; }
        public Double getPresencePenalty() { return presencePenalty; }
        public void setPresencePenalty(Double presencePenalty) { this.presencePenalty = presencePenalty; }
        public Boolean getStream() { return stream; }
        public void setStream(Boolean stream) { this.stream = stream; }
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
