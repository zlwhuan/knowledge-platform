package com.company.knowledge.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for RAG service integration
 */
@Configuration
public class RagConfig {

    @Value("${rag.service.timeout:30000}")
    private int timeout;

    @Value("${rag.service.url:http://localhost:8081}")
    private String ragServiceUrl;

    @Bean
    public RestTemplate ragRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return new RestTemplate(factory);
    }

    public String getRagServiceUrl() {
        return ragServiceUrl;
    }
}