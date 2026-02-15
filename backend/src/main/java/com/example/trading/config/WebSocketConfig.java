package com.example.trading.config;

import com.example.trading.service.QuoteStreamService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final QuoteStreamService quoteStreamService;

    public WebSocketConfig(QuoteStreamService quoteStreamService) {
        this.quoteStreamService = quoteStreamService;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(quoteStreamService, "/ws/quotes")
            .setAllowedOrigins("http://localhost:3000", "http://localhost");
    }
}
