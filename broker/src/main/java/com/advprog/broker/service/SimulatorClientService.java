package com.advprog.broker.service;

import com.advprog.broker.dto.BrokerMessage;
import com.advprog.broker.dto.SensorMeasurement;
import com.advprog.broker.dto.SensorSummary;
import com.advprog.broker.handler.BrokerStreamHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.List;

@Service
public class SimulatorClientService {

    private static final Logger log = LoggerFactory.getLogger(SimulatorClientService.class);

    private final SensorDiscoveryService sensorDiscoveryService;
    private final BrokerStreamHandler brokerStreamHandler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${simulator.base-url}")
    private String simulatorBaseUrl;

    public SimulatorClientService(SensorDiscoveryService sensorDiscoveryService,
                                   BrokerStreamHandler brokerStreamHandler) {
        this.sensorDiscoveryService = sensorDiscoveryService;
        this.brokerStreamHandler = brokerStreamHandler;
    }

    @PostConstruct
    public void connectToSensors() {
        List<SensorSummary> sensors = sensorDiscoveryService.getSensors();
        log.info("Connecting to {} simulator sensor WebSockets", sensors.size());

        for (SensorSummary sensor : sensors) {
            String wsUrl = toWsUrl(simulatorBaseUrl, sensor.websocket_url());
            log.info("Connecting to simulator WS for {}: {}", sensor.id(), wsUrl);

            StandardWebSocketClient client = new StandardWebSocketClient();
            client.execute(new TextWebSocketHandler() {
                @Override
                protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                    try {
                        SensorMeasurement measurement = objectMapper.readValue(
                                message.getPayload(), SensorMeasurement.class);
                        BrokerMessage brokerMessage = new BrokerMessage(
                                sensor.id(), measurement.timestamp(), measurement.value());
                        String json = objectMapper.writeValueAsString(brokerMessage);
                        brokerStreamHandler.broadcast(json);
                    } catch (Exception e) {
                        log.warn("Failed to process message from {}: {}", sensor.id(), e.getMessage());
                    }
                }

                @Override
                public void afterConnectionClosed(WebSocketSession session,
                        org.springframework.web.socket.CloseStatus status) {
                    log.warn("Simulator WS closed for {}: {}", sensor.id(), status);
                }
            }, new WebSocketHttpHeaders(), URI.create(wsUrl));
        }
    }

    private String toWsUrl(String baseUrl, String path) {
        String wsBase = baseUrl.replaceFirst("^http://", "ws://")
                               .replaceFirst("^https://", "wss://");
        return wsBase + path;
    }
}
