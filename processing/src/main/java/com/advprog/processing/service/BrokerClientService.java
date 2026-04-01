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

HEAD
    private volatile boolean brokerConnected = false;
    private volatile boolean connecting = false;

    private static final double SAMPLING_RATE_HZ = 20.0;


    private final ClassificationService classificationService;
    private final FftAnalysisService fftAnalysisService;
old-private-repo/processing-classification
    private final SlidingWindowService slidingWindowService;
    private final SensorCacheService sensorCacheService;
    private final FftAnalysisService fftAnalysisService;
    private final ClassificationService classificationService;
    private final EventPersistenceService eventPersistenceService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EventPersistenceService persistenceService;
    @Value("${broker.url}")
    private String brokerUrl;

HEAD
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

    public BrokerClientService(ClassificationService classificationService,
                               FftAnalysisService fftAnalysisService,
                               SlidingWindowService slidingWindowService,
                               SensorCacheService sensorCacheService, 
                               EventPersistenceService persistenceService) {
        this.classificationService = classificationService;
        this.fftAnalysisService = fftAnalysisService;
        this.slidingWindowService = slidingWindowService;
        this.sensorCacheService = sensorCacheService;
        this.persistenceService=persistenceService;
old-private-repo/processing-classification
    }

    @PostConstruct
    public void connect() {
        Thread thread = new Thread(() -> {
            while (true) {
                if (!brokerConnected && !connecting) {
                    doConnect();
                }
                try {
HEAD
                    Thread.sleep(5_000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;

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

                                FftAnalysisService.FftResult fftResult =
                                        fftAnalysisService.analyze(result.samples(), samplingRateHz);

                                String classification =
                                        classificationService.classify(result.sensorId(), fftResult);

                                if (classification == null) return;

                                log.info("Event type:{} (freq={} Hz, magnitude={})",
                                       
                                        classification,
                                        fftResult.dominantFrequencyHz(),
                                        fftResult.magnitude());
                            persistenceService.save(result.sensorId(), sensor, classification,  fftResult.dominantFrequencyHz(),   fftResult.magnitude(),
                             result.windowStart(), result.windowEnd());
                                       
                            });
                           
                } catch (Exception e) {
                    log.warn("Failed to process broker message: {}", e.getMessage());
old-private-repo/processing-classification
                }
            }
        }, "broker-ws-manager");
        thread.setDaemon(true);
        thread.start();
    }

    private void doConnect() {
        connecting = true;
        String url = brokerUrl + "/stream";
        log.info("BrokerClientService: connecting to broker WS at {}", url);
        StandardWebSocketClient client = new StandardWebSocketClient();
        try {
            client.execute(new TextWebSocketHandler() {
                @Override
                public void afterConnectionEstablished(WebSocketSession session) {
                    brokerConnected = true;
                    connecting = false;
                    log.info("BrokerClientService: broker WS connected");
                }

                @Override
                public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
                    brokerConnected = false;
                    connecting = false;
                    log.warn("BrokerClientService: broker WS closed: {}", status);
                }

                @Override
                public void handleTransportError(WebSocketSession session, Throwable exception) {
                    brokerConnected = false;
                    connecting = false;
                    log.warn("BrokerClientService: transport error — {}", exception.getMessage());
                }

                @Override
                protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                    try {
                        BrokerMessage msg = objectMapper.readValue(
                                message.getPayload(), BrokerMessage.class);
                        slidingWindowService.addSample(msg.sensorId(), msg.value(), msg.timestamp())
                                .ifPresent(result -> {
                                    SensorSummary sensor0 = sensorCacheService.get(result.sensorId()).orElse(null);
                                    String sensorName = sensor0 != null ? sensor0.name() : "unknown";

                                    double samplingRate = sensor0 != null ? sensor0.samplingRateHz() : SAMPLING_RATE_HZ;
                                    FftAnalysisService.FftResult fft =
                                            fftAnalysisService.analyze(result.samples(), samplingRate);

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
                                                                sensor != null ? sensor.category() : null
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
        } catch (Exception e) {
            log.warn("BrokerClientService: connect failed — {}", e.getMessage());
            connecting = false;
        }
    }

    public boolean isBrokerConnected() { return brokerConnected; }
}
