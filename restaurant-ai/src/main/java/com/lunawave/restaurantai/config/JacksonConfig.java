package com.lunawave.restaurantai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        // JsonMapper is the modern Jackson builder-based ObjectMapper
        return JsonMapper.builder().build();
    }
}
