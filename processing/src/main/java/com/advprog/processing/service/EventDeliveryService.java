package com.advprog.processing.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.advprog.processing.dto.Event;
import com.advprog.processing.ws.EventsWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class EventDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(EventDeliveryService.class);

    private final EventsWebSocketHandler wsHandler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ExecutorService dispatcher = Executors.newSingleThreadExecutor();

    public EventDeliveryService(EventsWebSocketHandler wsHandler) {
        this.wsHandler = wsHandler;
    }

    public void publish(Event event) {
        dispatcher.execute(() -> {
            try {
                String json = objectMapper.writeValueAsString(event);
                TextMessage msg = new TextMessage(json);

                int sent = 0;
                for (WebSocketSession s : wsHandler.sessions()) {
                    try {
                        if (s.isOpen()) {
                            s.sendMessage(msg);
                            sent++;
                        }
                    } catch (Exception sendEx) {
                        try { s.close(); } catch (Exception ignore) {}
                    }
                }

                log.debug("Published event id={} to {} session(s) (connected={})",
                        event.id(), sent, wsHandler.sessionCount());
            } catch (Exception e) {
                log.warn("Failed to publish event id={}: {}", event.id(), e.getMessage());
            }
        });
    }
}