run it and test:
```bash
websocat ws://localhost:8081/stream | head -5
# expect: 5 lines of {"sensorId":"sensor-XX","timestamp":"...","value":...}
```
---

# Broker
The broker must satisfy exactly two rules:

1. Capture incoming measurements from the simulator
2. Redistribute them to all processing replicas via broadcast

it must NOT perform any data processing. No FFT, no classification, no database access. **Only forwarding**.

---

## Feature F1 — Sensor discovery at startup

A @Service class that, when the application starts, calls GET http://simulator:8080/api/devices/ and deserializes the response into a List<SensorSummary>.
To be able to see if it's working, it has to display logs.

```java
// DTO matching the simulator's response
public record SensorSummary(
    String id,
    String name,
    String category,
    String region,
    Coordinates coordinates,
    String measurement_unit,
    double sampling_rate_hz,
    String websocket_url
) {}

public record Coordinates(double latitude, double longitude) {}
```

**How to test it:** Run the broker app, check the logs. You should see:
```
Discovered 12 sensors: [sensor-01, sensor-02, ..., sensor-12]

and

INFO:     192.168.65.1:46813 - "GET /api/devices/ HTTP/1.1" 200 OK
```

---

## Feature F2 — Connect to 12 sensor WebSockets

For each SensorSummary from F1, open a WebSocket connection to ws://simulator:8080{sensor.websocket_url}. Register a message handler that:

1. Deserializes the incoming JSON into SensorMeasurement
2. Wraps it with the sensorId to create a BrokerMessage

```java
// Simulator's DTO (no sensorId)
public record SensorMeasurement(
    String timestamp,
    double value
) {}

// Our enriched DTO (adds sensorId)
public record BrokerMessage(
    String sensorId,
    String timestamp,
    double value
) {}
```

**How to test it:** Run the broker and check logs. You should see interleaved messages from all 12 sensors:
```bash
[sensor-01] timestamp=2026-03-28T14:00:00.050Z value=0.031
[sensor-05] timestamp=2026-03-28T14:00:00.050Z value=-0.012
```

---

## Feature F3 — Accept replica WebSocket connections

A Spring WebSocket endpoint at `/stream` that accepts incoming connections and stores each session in a thread-safe set (e.g., ConcurrentHashMap.newKeySet()). When a client disconnects, remove it from the set.

```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(brokerWebSocketHandler(), "/stream")
                .setAllowedOrigins("*");
    }
}
```

**How to test it**: Start the broker, then connect with websocat:

```bash
websocat ws://localhost:8081/stream
```
Check the broker logs — you should see "Replica connected, total: 1". Connect a second websocat and see "total: 2". Disconnect one and see "total: 1".

---

## Feature F4 — Broadcast to all connected replicas

In the message handler from F2 (where you receive SensorMeasurement from the simulator), serialize the BrokerMessage to JSON and send it to every session in the set from F3. Handle the case where a session has been closed (catch the exception, remove it from the set).

```java
// pseudocode inside the simulator WS handler
void onMessage(String sensorId, SensorMeasurement measurement) {
    BrokerMessage msg = new BrokerMessage(sensorId, measurement.timestamp(), measurement.value());
    String json = objectMapper.writeValueAsString(msg);
    
    for (WebSocketSession session : replicaSessions) {
        try {
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            replicaSessions.remove(session);
        }
    }
}
```

**How to test it**: Open two websocat clients connected to `ws://localhost:8081/stream`. Both should receive the same stream of BrokerMessage JSON objects at ~240 messages/second. Each message should have three fields: sensorId, timestamp, value.
```bash
# terminal 1
websocat ws://localhost:8081/stream | head -5

# terminal 2 (simultaneously)
websocat ws://localhost:8081/stream | head -5
```
Both should show the same data flowing. If one receives and the other doesn't, there's a bug in the broadcast loop.

---
