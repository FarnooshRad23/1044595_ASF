package com.advprog.broker.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BrokerStreamHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(BrokerStreamHandler.class);

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("Replica connected: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("Replica disconnected: {}", session.getId());
    }

    public void broadcast(String json) {
        TextMessage msg = new TextMessage(json);
        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    synchronized (session) {
                        session.sendMessage(msg);
                    }
                }
            } catch (IOException e) {
                log.warn("Removing dead replica session {}: {}", session.getId(), e.getMessage());
                sessions.remove(session);
            }
        }
    }
}
