package com.example.chat_api.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // For Postman/cURL testing. Remove later or scope it to /magic/** only.
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/magic/**", "/error", "/", "/css/**", "/js/**", "/ai/**").permitAll()
                        .anyRequest().permitAll()   // or .authenticated() if you have other secured endpoints
                );
        return http.build();
    }
}
