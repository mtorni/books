package com.lunawave.restaurantai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.lunawave.restaurantai.voice.TwilioOpenAiRealtimeWebSocketHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final TwilioOpenAiRealtimeWebSocketHandler voiceHandler;

    public WebSocketConfig(TwilioOpenAiRealtimeWebSocketHandler voiceHandler) {
        this.voiceHandler = voiceHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(voiceHandler, "/api/voice/*/stream")
            .setAllowedOrigins("*");
    }
}
