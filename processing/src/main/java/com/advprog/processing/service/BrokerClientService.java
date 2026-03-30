package com.advprog.processing.service;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.advprog.processing.dto.BrokerMessage;
import com.advprog.processing.dto.SensorSummary;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

@Service
public class BrokerClientService {

    private static final Logger log = LoggerFactory.getLogger(BrokerClientService.class);
    
    private final ClassificationService classificationService;
    private final SlidingWindowService slidingWindowService;
    private final SensorCacheService sensorCacheService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${broker.url}")
    private String brokerUrl;

    public BrokerClientService(ClassificationService classificationService,SlidingWindowService slidingWindowService,
                                SensorCacheService sensorCacheService) {
        this.classificationService=classificationService;                             
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
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                try {
                    BrokerMessage msg = objectMapper.readValue(
                            message.getPayload(), BrokerMessage.class);
                    slidingWindowService.addSample(msg.sensorId(), msg.value(), msg.timestamp())
                            .ifPresent(result -> {
                                SensorSummary sensor = sensorCacheService.get(result.sensorId())
                                        .orElse(null);
                                String sensorName = sensor != null ? sensor.name() : "unknown";
                                double samplingRateHz = sensor != null ? sensor.samplingRateHz() : 100.0;
                                log.info("Window full for {} [{}] — windowStart={} windowEnd={}",
                                        result.sensorId(), sensorName,
                                        result.windowStart(), result.windowEnd());
                                String eventType = classificationService.classify(result, samplingRateHz);
                                if (eventType == null) return; // noise — discard
                            });
                } catch (Exception e) {
                    log.warn("Failed to process broker message: {}", e.getMessage());
                }
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session,
                    org.springframework.web.socket.CloseStatus status) {
                log.warn("Broker WS connection closed: {}", status);
            }
        }, new WebSocketHttpHeaders(), URI.create(url));
    }
}
