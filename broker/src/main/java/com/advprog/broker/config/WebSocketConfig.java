package com.advprog.broker.config;

import com.advprog.broker.handler.BrokerStreamHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final BrokerStreamHandler brokerStreamHandler;

    public WebSocketConfig(BrokerStreamHandler brokerStreamHandler) {
        this.brokerStreamHandler = brokerStreamHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(brokerStreamHandler, "/stream");
    }
}
