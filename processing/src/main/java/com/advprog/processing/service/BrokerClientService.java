package com.advprog.processing.service;

import java.net.URI;

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

import com.advprog.processing.dto.BrokerMessage;
import com.advprog.processing.dto.SensorSummary;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

@Service
public class BrokerClientService {

    private static final Logger log = LoggerFactory.getLogger(BrokerClientService.class);

    private volatile boolean brokerConnected = false;

    private static final double SAMPLING_RATE_HZ = 20.0;

    private final SlidingWindowService slidingWindowService;
    private final SensorCacheService sensorCacheService;
    private final FftAnalysisService fftAnalysisService;
    private final ClassificationService classificationService;
    private final EventPersistenceService eventPersistenceService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${broker.url}")
    private String brokerUrl;

    public BrokerClientService(SlidingWindowService slidingWindowService,
                                SensorCacheService sensorCacheService,
                                FftAnalysisService fftAnalysisService,
                                ClassificationService classificationService,
                                EventPersistenceService eventPersistenceService) {
        this.slidingWindowService = slidingWindowService;
        this.sensorCacheService = sensorCacheService;
        this.fftAnalysisService = fftAnalysisService;
        this.classificationService = classificationService;
        this.eventPersistenceService = eventPersistenceService;
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
                                // window is full, ready for analysis

                                SensorSummary sensor0 = sensorCacheService.get(result.sensorId()).orElse(null);
                                String sensorName = sensor0 != null ? sensor0.name() : "unknown";

                                FftAnalysisService.FftResult fft =
                                        fftAnalysisService.analyze(result.samples(), SAMPLING_RATE_HZ);
                                log.info("dominand freq: {}",
                                        fft.dominantFreqHz());
                                log.info("Window complete: sensor={} [{}] region={} - dominand freq: {}",
                                        result.sensorId(), sensorName,
                                        sensor0 != null ? sensor0.region() : "unknown",
                                        fft.dominantFreqHz());
                                classificationService.classify(fft.dominantFreqHz())
                                        .ifPresentOrElse(
                                                eventType -> {
                                                    SensorSummary sensor = sensor0;
                                                    log.info("EVENT DETECTED: sensor={} [{}] type={} freq={}Hz mag={}",
                                                            result.sensorId(), sensorName,
                                                            eventType, fft.dominantFreqHz(), fft.magnitude());
                                                    eventPersistenceService.persist(
                                                            result.sensorId(),
                                                            sensor != null ? sensor.name()     : "unknown",
                                                            eventType,
                                                            fft.dominantFreqHz(),
                                                            fft.magnitude(),
                                                            result.windowStart(),
                                                            result.windowEnd(),
                                                            sensor != null ? sensor.region()   : null,
                                                            sensor != null ? sensor.category() : null,
                                                            sensor.coordinates(),
                                                            msg.timestamp()
                                                    );
                                                },
                                                () -> log.debug("Noise discarded: sensor={} freq={}Hz",
                                                        result.sensorId(), fft.dominantFreqHz())
                                        );
                            });
                } catch (Exception e) {
                    log.warn("Failed to process broker message: {}", e.getMessage());
                }
            }

        }, new WebSocketHttpHeaders(), URI.create(url));
    }

    public boolean isBrokerConnected() { return brokerConnected; }
}
