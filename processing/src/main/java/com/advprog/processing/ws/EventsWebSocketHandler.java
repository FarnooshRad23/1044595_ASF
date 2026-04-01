package com.advprog.processing.ws;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class EventsWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        WebSocketSession safe = new ConcurrentWebSocketSessionDecorator(
                session,
                10_000,      // send-time limit (ms)
                512 * 1024   // buffer limit (bytes)
        );
        sessions.put(session.getId(), safe);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
    }

    public Iterable<WebSocketSession> sessions() {
        return sessions.values();
    }

    public int sessionCount() {
        return sessions.size();
    }
}