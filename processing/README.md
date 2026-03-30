processing replica has exactly one workflow: data comes in → math happens → row goes to database.

Separation of concerns:
- **Separate what changes for different reasons**. The FFT math shouldn't know about WebSockets. The database code shouldn't know about frequency bands. This is just good layering, not DDD-specific.
- **Keep the domain logic pure**. The classification rules (frequency → event type) should be in a plain Java class with no Spring annotations, no database imports, no WebSocket imports. Just input → output. This makes it testable and easy to understand.

---

# Layers

Let's think of the replica as a linear chain of services (Spring @Service), each calling the next:
```
WS Client → SlidingWindow → FFT → Classifier → Persistence → Dashboard push
```

## Layer 1 — Ingestion (infrastructure).
Two clients that connect to external systems: a WebSocket client to the broker, and an SSE client to the simulator's control stream. These are pure infrastructure — they receive bytes, deserialize them, and hand off to the next layer.

## Layer 2 — Buffering (stateful service).
The **sliding window**. It holds a `Map<String, Deque<Double>>` — 12 queues, one per sensor. When a measurement arrives, it goes into the right queue. When a queue reaches 128 samples, it triggers analysis. This is the only stateful part of the system.

## Layer 3 — Analysis (pure logic).
FFT + classification. Takes `double[128]` in, returns either "no event" or a `DetectedEvent` object. Zero dependencies on Spring, database, or network. You could unit-test this with a handwritten sine wave array.

## Layer 4 — Persistence (infrastructure).
Takes a `DetectedEvent`, writes it to PostgreSQL with ON CONFLICT DO NOTHING. Also pushes the event to connected dashboard WebSocket clients for real-time updates.

## Layer 5 — API (controller).
REST endpoints for the dashboard to query historical events, plus the health check for Nginx.

---

# Data Flow
When a BrokerMessage arrives, this is the exact sequence of method calls:

```text
BrokerClientService.onMessage(json)
  → slidingWindowService.addSample(sensorId, value, timestamp)
      → buffer for that sensor adds the value
      → if buffer.size() == 128:
            double[] samples = buffer.drain()  // returns 128 values and clears
            → fftService.analyze(samples, samplingRateHz)
                → returns {dominantFreqHz, magnitude}
            → classificationService.classify(dominantFreqHz, magnitude)
                → returns Optional<EventType>  (empty if noise)
            → if present:
                  SensorSummary meta = sensorCache.get(sensorId)
                  → persistenceService.save(sensorId, meta, eventType, freq, magnitude, windowStart, windowEnd)
                      → INSERT ON CONFLICT DO NOTHING
                      → if saved (not duplicate):
                            dashboardPushService.broadcast(eventResponse)
```
Every class has one job and calls the next class down.

---

# Key design decisions

**Non-overlapping windows**:
The buffer collects 128 samples, triggers FFT, clears completely, and starts fresh. Analysis happens once every 6.4 seconds per sensor (collecting 128 samples).
This means the `SlidingWindowService` uses a `List<Double>` (not a `Deque`), and the trigger condition is simply `list.size() == 128`. 

**WebSocket push to dashboard**:
When an event is persisted successfully (not a duplicate), the `DashboardPushService` immediately serializes the `EventResponse` and sends it to all connected WebSocket sessions.
The dashboard gets instant updates without polling every 2-3 seconds.

**Autonomous sensor cache**:
Each replica calls `GET /api/devices/` at startup and builds a `ConcurrentHashMap<String, SensorSummary>`. 
On cache miss (unlikely but possible), it does a lazy fetch. The broker never touches metadata.

**Brutal shutdown**:
`ControlStreamService` receives the SSE stream. On `{"command":"SHUTDOWN"}`, it calls `System.exit(1).` Docker's `restart: on-failure` brings the container back.
No graceful flush.

---
**note**: I need to remember **window timestamps**
```java
// inside SlidingWindowService, per sensor:
List<Double> values;       // the 128 sample values
String firstTimestamp;      // timestamp of values[0]
String lastTimestamp;       // timestamp of values[127]
```
These become window_start and window_end in the database row are part of the deduplication key.
When the buffer drains, it returns: the double[128] for FFT, and the two timestamps for persistence.

---

# Road map

1. SensorCacheService — call /api/devices/, build the map. Test: log 12 entries at startup.
2. BrokerClientService + SlidingWindowService — receive from broker, fill buffers. Test: log "window full for sensor-03" every ~6.4 seconds.
3. FftAnalysisService — pure math. Test: feed it a synthetic sine wave at 2 Hz, verify it returns ~2 Hz.
4. ClassificationService — frequency band rules. Test: unit test with known frequencies (1.5 → earthquake, 5.0 → explosion, 9.0 → nuclear, 0.3 → null).
5. EventPersistenceService — database writes. Test: inject an earthquake via simulator admin API, verify row appears in PostgreSQL.
6. ControlStreamService — SSE listener + shutdown. Test: trigger POST /api/admin/shutdown, verify the replica process dies and Docker restarts it.

Then add the API layer (EventController, HealthController, DashboardPushService) last