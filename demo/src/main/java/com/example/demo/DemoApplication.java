package com.example.demo;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(
	info = @Info(
		title = "Intelligent Memory Preservation & Retrieval System (IMPRS) API",
		version = "1.0.0",
		description = "Enterprise-grade AI-powered platform utilizing advanced RAG architecture, pgvector semantic search, NVIDIA NIM embeddings, and LLM-based retrieval to help Alzheimer's and dementia patients preserve, organize, and recall their personal memories and life experiences"
	)
)
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

}
