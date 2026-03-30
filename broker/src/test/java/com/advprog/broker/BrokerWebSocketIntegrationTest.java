package com.advprog.broker;

import com.advprog.broker.dto.SensorSummary;
import com.advprog.broker.handler.BrokerStreamHandler;
import com.advprog.broker.service.SensorDiscoveryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Boots the full Spring context on a random port. SensorDiscoveryService is mocked to return
 * an empty sensor list so SimulatorClientService attempts no outbound WS connections.
 *
 * Verifies end-to-end: a test WS client connects to /stream, calls broadcast(),
 * and asserts the message arrives.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BrokerWebSocketIntegrationTest {

    @MockitoBean
    SensorDiscoveryService sensorDiscoveryService;

    @Autowired
    BrokerStreamHandler brokerStreamHandler;

    @LocalServerPort
    int port;

    @Test
    void connectedClientReceivesBroadcast() throws Exception {
        when(sensorDiscoveryService.getSensors()).thenReturn(List.of());

        BlockingQueue<String> received = new LinkedBlockingQueue<>();

        StandardWebSocketClient client = new StandardWebSocketClient();
        client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                received.add(message.getPayload());
            }
        }, new WebSocketHttpHeaders(), URI.create("ws://localhost:" + port + "/stream"))
              .get(5, TimeUnit.SECONDS);

        // Give the server side a moment to register the session
        Thread.sleep(200);

        String json = "{\"sensorId\":\"sensor-03\",\"timestamp\":\"2026-01-01T00:00:00Z\",\"value\":0.5}";
        brokerStreamHandler.broadcast(json);

        String msg = received.poll(3, TimeUnit.SECONDS);
        assertNotNull(msg, "Client should have received a message within 3 s");
        assertEquals(json, msg);
    }

    @Test
    void multipleClientsAllReceiveBroadcast() throws Exception {
        when(sensorDiscoveryService.getSensors()).thenReturn(List.of());

        BlockingQueue<String> inbox1 = new LinkedBlockingQueue<>();
        BlockingQueue<String> inbox2 = new LinkedBlockingQueue<>();

        StandardWebSocketClient client = new StandardWebSocketClient();

        client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession s, TextMessage m) {
                inbox1.add(m.getPayload());
            }
        }, new WebSocketHttpHeaders(), URI.create("ws://localhost:" + port + "/stream"))
              .get(5, TimeUnit.SECONDS);

        client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession s, TextMessage m) {
                inbox2.add(m.getPayload());
            }
        }, new WebSocketHttpHeaders(), URI.create("ws://localhost:" + port + "/stream"))
              .get(5, TimeUnit.SECONDS);

        Thread.sleep(200);

        brokerStreamHandler.broadcast("{\"sensorId\":\"sensor-01\",\"timestamp\":\"t\",\"value\":1.0}");

        assertNotNull(inbox1.poll(3, TimeUnit.SECONDS), "Replica 1 should receive message");
        assertNotNull(inbox2.poll(3, TimeUnit.SECONDS), "Replica 2 should receive message");
    }
}
