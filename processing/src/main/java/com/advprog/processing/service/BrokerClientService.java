package com.advprog.processing.service;

import com.advprog.processing.dto.BrokerMessage;
import com.advprog.processing.dto.SensorSummary;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;

@Service
public class BrokerClientService {

    private static final Logger log = LoggerFactory.getLogger(BrokerClientService.class);

    private volatile boolean brokerConnected = false;

    private final SlidingWindowService slidingWindowService;
    private final SensorCacheService sensorCacheService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${broker.url}")
    private String brokerUrl;

    public BrokerClientService(SlidingWindowService slidingWindowService,
                                SensorCacheService sensorCacheService) {
        this.slidingWindowService = slidingWindowService;
        this.sensorCacheService = sensorCacheService;
    }

    @PostConstruct
    public void connect() {
        String url = brokerUrl + "/stream";
        log.info("Connecting to broker at {}", url);

        StandardWebSocketClient client = new StandardWebSocketClient();
        client.execute(new TextWebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) {
                brokerConnected = true;
                log.info("Broker WS connection established");
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
                brokerConnected = false;
                log.warn("Broker WS connection closed: {}", status);
            }

            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                try {
                    BrokerMessage msg = objectMapper.readValue(
                            message.getPayload(), BrokerMessage.class);
                    slidingWindowService.addSample(msg.sensorId(), msg.value(), msg.timestamp())
                            .ifPresent(result -> {
                                String sensorName = sensorCacheService.get(result.sensorId())
                                        .map(SensorSummary::name)
                                        .orElse("unknown");
                                log.info("Window full for {} [{}] — windowStart={} windowEnd={}",
                                        result.sensorId(), sensorName,
                                        result.windowStart(), result.windowEnd());
                            });
                } catch (Exception e) {
                    log.warn("Failed to process broker message: {}", e.getMessage());
                }
            }

        }, new WebSocketHttpHeaders(), URI.create(url));
    }

    public boolean isBrokerConnected() { return brokerConnected; }
}
