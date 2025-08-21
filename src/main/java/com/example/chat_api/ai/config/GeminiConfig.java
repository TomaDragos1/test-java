package com.example.chat_api.ai.config;

import com.google.genai.Client;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiConfig {

    @Bean
    public Client genAiClient() {
        // Uses GOOGLE_API_KEY from environment
        return new Client();
    }
}
