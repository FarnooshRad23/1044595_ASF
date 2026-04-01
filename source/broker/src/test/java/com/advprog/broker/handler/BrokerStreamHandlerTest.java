package com.advprog.broker.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class BrokerStreamHandlerTest {

    private BrokerStreamHandler handler;

    @BeforeEach
    void setUp() {
        handler = new BrokerStreamHandler();
    }

    // ── session lifecycle ─────────────────────────────────────────────────────

    @Test
    void afterConnectionEstablished_addsSession() throws Exception {
        WebSocketSession session = openSession("s1");

        handler.afterConnectionEstablished(session);
        handler.broadcast("{\"sensorId\":\"sensor-01\"}");

        verify(session).sendMessage(new TextMessage("{\"sensorId\":\"sensor-01\"}"));
    }

    @Test
    void afterConnectionClosed_removesSession() throws Exception {
        WebSocketSession session = openSession("s1");

        handler.afterConnectionEstablished(session);
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);
        handler.broadcast("{}");

        verify(session, never()).sendMessage(any());
    }

    // ── broadcast ─────────────────────────────────────────────────────────────

    @Test
    void broadcast_sendsToAllOpenSessions() throws Exception {
        WebSocketSession s1 = openSession("s1");
        WebSocketSession s2 = openSession("s2");
        WebSocketSession s3 = openSession("s3");

        handler.afterConnectionEstablished(s1);
        handler.afterConnectionEstablished(s2);
        handler.afterConnectionEstablished(s3);

        String json = "{\"sensorId\":\"sensor-05\",\"timestamp\":\"t\",\"value\":1.5}";
        handler.broadcast(json);

        TextMessage expected = new TextMessage(json);
        verify(s1).sendMessage(expected);
        verify(s2).sendMessage(expected);
        verify(s3).sendMessage(expected);
    }

    @Test
    void broadcast_skipsClosedSession() throws Exception {
        WebSocketSession open = openSession("open");
        WebSocketSession closed = mock(WebSocketSession.class);
        when(closed.getId()).thenReturn("closed");
        when(closed.isOpen()).thenReturn(false);

        handler.afterConnectionEstablished(open);
        handler.afterConnectionEstablished(closed);
        handler.broadcast("{}");

        verify(open).sendMessage(any());
        verify(closed, never()).sendMessage(any());
    }

    @Test
    void broadcast_removesDeadSessionOnIOException() throws Exception {
        WebSocketSession dead = openSession("dead");
        doThrow(new IOException("broken pipe")).when(dead).sendMessage(any());

        handler.afterConnectionEstablished(dead);

        // first broadcast: IOException caught, session removed
        handler.broadcast("{}");

        // second broadcast: dead session is gone, sendMessage not called again
        handler.broadcast("{}");

        verify(dead, times(1)).sendMessage(any());
    }

    @Test
    void broadcast_continuesAfterOneDeadSession() throws Exception {
        WebSocketSession dead = openSession("dead");
        WebSocketSession alive = openSession("alive");
        doThrow(new IOException("broken")).when(dead).sendMessage(any());

        handler.afterConnectionEstablished(dead);
        handler.afterConnectionEstablished(alive);
        handler.broadcast("{}");

        // alive must still receive the message despite dead throwing
        verify(alive).sendMessage(any());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private WebSocketSession openSession(String id) {
        WebSocketSession s = mock(WebSocketSession.class);
        lenient().when(s.getId()).thenReturn(id);
        lenient().when(s.isOpen()).thenReturn(true);
        return s;
    }
}
