# Event schema and data interfaces

This document defines every data structure exchanged between the components, from the raw sensor measurement produced by the simulator to the final detected event shown on the dashboard. Each section covers one hop in the pipeline, specifying the exact JSON format, the protocol used, and the responsibility of the sender and receiver.

---

## 1. Pipeline overview

A seismic measurement travels through five transformation stages before reaching the analyst's screen:

```
Simulator ──[SensorMeasurement]──▶ Broker ──[BrokerMessage]──▶ Replica
                                                                  │
                                                          Sliding window
                                                                  │
                                                           FFT analysis
                                                                  │
                                                          Classification
                                                                  │
                                                      [DetectedEvent] ──▶ PostgreSQL
                                                                              │
                                                                    [EventResponse] ──▶ Dashboard
```

In parallel, a separate control path handles replica failures:

```
Simulator ──[ShutdownCommand via SSE]──▶ Replica ──▶ System.exit(0) ──▶ Docker restarts it
```

Below, each hop is documented in order.

---

## 2. Simulator → Broker: sensor discovery (one-time at startup)

**Protocol:** HTTP GET  
**Endpoint:** `GET http://simulator:8080/api/devices/`  
**Direction:** Broker calls the simulator  
**When:** Once, when the broker container starts  
**Purpose:** Learn which sensors exist and obtain their WebSocket URLs

### Request

No body. Simple GET request.

### Response: `SensorSummary[]`

```json
[
  {
    "id": "sensor-08",
    "name": "DC North Perimeter",
    "category": "datacenter",
    "region": "Replica Datacenter",
    "coordinates": {
      "latitude": 45.4642,
      "longitude": 9.19
    },
    "measurement_unit": "mm/s",
    "sampling_rate_hz": 20.0,
    "websocket_url": "/api/device/sensor-08/ws"
  }
]
```

| Field | Type | Description |
|---|---|---|
| id | string | Stable unique sensor identifier (e.g. `sensor-01`) |
| name | string | Human-readable name (e.g. `DC North Perimeter`) |
| category | string | Either `field` (remote surveillance) or `datacenter` (datacenter-hosted) |
| region | string | Logical geographic area the sensor belongs to |
| coordinates | object | `{latitude: float, longitude: float}` — geographic position |
| measurement_unit | string | Always `mm/s` |
| sampling_rate_hz | float | Samples per second (default 20.0) |
| websocket_url | string | Relative WebSocket path for this sensor's data stream |

**What the broker does with this:** It stores the full list of `SensorSummary` objects in memory (it will need the `id` to enrich measurements later). It then opens a WebSocket connection to each sensor using `websocket_url`.

---

## 3. Simulator → Broker: sensor measurement stream (continuous)

**Protocol:** WebSocket  
**Endpoint:** `WS simulator:8080/api/device/{sensor_id}/ws` (one connection per sensor)  
**Direction:** Broker connects to the simulator (12 parallel WebSocket connections)  
**When:** Continuously after startup  
**Rate:** One message every 50ms per sensor (at default 20 Hz), so 12 × 20 = 240 messages/second total  
**Purpose:** Receive the raw time-domain signal from each sensor

### Message: `SensorMeasurement`

This is the DTO defined by the simulator contract. Each WebSocket frame contains exactly one measurement:

```json
{
  "timestamp": "2026-03-28T14:00:00.050000+00:00",
  "value": 0.042871
}
```

| Field | Type | Description |
|---|---|---|
| timestamp | string | UTC ISO-8601 timestamp of when the sample was taken |
| value | float | Signed ground velocity in mm/s at that instant |

**Important:** This DTO does NOT include the sensor ID. The broker knows which sensor a measurement belongs to because each WebSocket connection is dedicated to a single sensor. The broker must track the association `WebSocket connection → sensor_id` internally.

**What the broker does with this:** It wraps the measurement with the corresponding `sensor_id` to produce a `BrokerMessage`, then broadcasts it to all connected replicas.

---

## 4. Broker → Processing replicas: enriched measurement broadcast (continuous)

**Protocol:** WebSocket  
**Endpoint:** `WS broker:8081/stream` (custom endpoint, we define this)  
**Direction:** Each processing replica connects to the broker  
**When:** Continuously after startup  
**Rate:** ~240 messages/second (all 12 sensors multiplexed into one stream)  
**Purpose:** Deliver sensor data to replicas without each replica needing 12 separate connections to the simulator

### Message: `BrokerMessage`

This is our own DTO, not from the simulator contract. It adds the `sensorId` so the replica knows which sensor's sliding window to update:

```json
{
  "sensorId": "sensor-03",
  "timestamp": "2026-03-28T14:00:00.050000+00:00",
  "value": 0.042871
}
```

| Field | Type | Description |
|---|---|---|
| sensorId | string | The sensor ID this measurement belongs to (from discovery) |
| timestamp | string | UTC ISO-8601 timestamp (passed through from `SensorMeasurement`) |
| value | float | Signed ground velocity in mm/s (passed through from `SensorMeasurement`) |

**Why this exists:** The simulator's `SensorMeasurement` has no sensor ID because the WebSocket path already implies it. But since the broker multiplexes all 12 sensors into a single stream to each replica, the replica needs to know which sensor each measurement came from. This enrichment is the broker's only job.

**What the replica does with this:** It routes the measurement to the correct sensor's sliding window buffer based on `sensorId`.

---

## 5. Simulator → Processing replicas: control stream (continuous, parallel path)

**Protocol:** Server-Sent Events (SSE)  
**Endpoint:** `GET http://simulator:8080/api/control`  
**Direction:** Each replica independently connects to the simulator (NOT through the broker)  
**When:** Continuously after startup  
**Purpose:** Receive shutdown commands that simulate replica failure

This is a separate data path from the measurement stream. Each replica maintains its own SSE connection directly to the simulator. The broker is not involved.

### SSE event types

The control stream emits three types of SSE events, distinguished by the `event:` field in the SSE protocol:

**Event type `control-open`** — emitted once when the connection is established:

```json
{
  "connectedAt": "2026-03-28T14:00:00.000000+00:00",
  "controlStreamConnections": 3
}
```

**Event type `heartbeat`** — emitted periodically (~every 15 seconds) to keep the connection alive:

```json
{
  "timestamp": "2026-03-28T14:00:15.000000+00:00",
  "controlStreamConnections": 3
}
```

**Event type `command`** — emitted when a shutdown is triggered (randomly between 30-90 seconds, configurable via `AUTO_SHUTDOWN_MIN_SECONDS` and `AUTO_SHUTDOWN_MAX_SECONDS`):

```json
{
  "command": "SHUTDOWN"
}
```

| SSE event type | DTO | Replica action |
|---|---|---|
| `control-open` | `ControlOpenEvent` | Log connection confirmation; no action needed |
| `heartbeat` | `ControlHeartbeatEvent` | Ignore (connection keepalive) |
| `command` | `ShutdownCommand` | Terminate immediately with `System.exit(0)` |

**Critical behavior:** When the simulator decides to trigger a shutdown, it sends the `ShutdownCommand` to **exactly one** connected SSE listener. Only one replica dies per shutdown event. The other replicas are unaffected and continue processing normally.

**What the replica does:**
- On `control-open`: optionally log that it connected successfully
- On `heartbeat`: nothing, ignore it
- On `command` with value `SHUTDOWN`: call `System.exit(0)` (or equivalent) immediately. Docker's `restart: on-failure` policy will restart the container automatically.

---

## 6. Replica internal: sliding window → FFT → classification

This section describes the data transformations that happen inside each processing replica, from receiving a `BrokerMessage` to producing a `DetectedEvent`.

### 6.1 Sliding window buffer

Each replica maintains one circular buffer per sensor (12 buffers total). Each buffer holds the last 128 `BrokerMessage` values for that sensor.

**Internal data structure** (conceptual, in Java):

```
Map<String, Deque<BrokerMessage>> windows
  key: sensorId (e.g. "sensor-03")
  value: circular buffer of the 128 most recent messages for that sensor
```

When a new `BrokerMessage` arrives:
1. Route it to the buffer for `message.sensorId`
2. Append it to the buffer
3. If the buffer exceeds 128 entries, remove the oldest entry
4. If the buffer now contains exactly 128 entries, trigger FFT analysis

### 6.2 FFT analysis

When a sensor's buffer is full (128 samples), the replica extracts the `value` field from each sample and runs an FFT.

**Input:** `double[128]` — the array of 128 velocity values in chronological order.

**Processing steps:**
1. Extract values: `double[] data = window.stream().map(m -> m.value).toArray()`
2. Run FFT: `Complex[] spectrum = fft.transform(data, TransformType.FORWARD)` (using Apache Commons Math `FastFourierTransformer`)
3. Compute magnitudes: for each bin `i`, `magnitude[i] = sqrt(re[i]² + im[i]²)`
4. Skip bin 0 (DC offset — the mean value, not a frequency)
5. Find the bin with the highest magnitude
6. Convert bin index to frequency: `dominantFrequencyHz = binIndex × samplingRateHz / windowSize`

**Output:** A frequency result:

```
dominantFrequencyHz: float   (e.g. 2.4)
magnitude: float              (e.g. 12.7)
```

**Key formulas:**

| Parameter | Formula | With defaults |
|---|---|---|
| Window size | 128 samples | 128 |
| Sampling rate | from simulator config | 20.0 Hz |
| Frequency resolution | samplingRate / windowSize | 20.0 / 128 = 0.15625 Hz per bin |
| Window duration | windowSize / samplingRate | 128 / 20.0 = 6.4 seconds |
| Max detectable frequency | samplingRate / 2 (Nyquist) | 10.0 Hz |

### 6.3 Classification

The dominant frequency is compared against the classification thresholds:

| Condition | Classified as | eventType value |
|---|---|---|
| f < 0.5 Hz | Background noise | **not persisted** (discarded) |
| 0.5 ≤ f < 3.0 Hz | Earthquake | `earthquake` |
| 3.0 ≤ f < 8.0 Hz | Conventional explosion | `conventional_explosion` |
| f ≥ 8.0 Hz | Nuclear-like event | `nuclear_like` |

Additionally, a magnitude threshold should be applied: if the dominant frequency's magnitude is below a noise floor (tuned empirically), the signal is discarded even if the frequency is within a classification band. This prevents classifying quiet noise as a false positive.

If the signal passes both checks (frequency in a band AND magnitude above threshold), a `DetectedEvent` is produced.

---

## 7. Replica → PostgreSQL: event persistence

**Protocol:** JDBC (PostgreSQL driver)  
**Connection:** `jdbc:postgresql://db:5432/sismaguard`  
**Direction:** Each replica writes to the same shared database  
**When:** Every time the classifier produces a `DetectedEvent`  
**Purpose:** Persist detected events with deduplication

### Database table: `detected_events`

```sql
CREATE TABLE detected_events (
    id               SERIAL PRIMARY KEY,
    sensor_id        VARCHAR(32)       NOT NULL,
    sensor_name      VARCHAR(128)      NOT NULL,
    event_type       VARCHAR(32)       NOT NULL,
    dominant_freq_hz DOUBLE PRECISION  NOT NULL,
    magnitude        DOUBLE PRECISION  NOT NULL,
    detected_at      TIMESTAMPTZ       NOT NULL DEFAULT NOW(),
    window_start     TIMESTAMPTZ       NOT NULL,
    window_end       TIMESTAMPTZ       NOT NULL,
    region           VARCHAR(128),
    category         VARCHAR(16),
    replica_id       VARCHAR(32),

    UNIQUE (sensor_id, event_type, window_start)
);

CREATE INDEX idx_events_detected_at ON detected_events (detected_at DESC);
CREATE INDEX idx_events_sensor      ON detected_events (sensor_id);
CREATE INDEX idx_events_type        ON detected_events (event_type);
```

| Column | Type | Source | Description |
|---|---|---|---|
| id | SERIAL | auto-generated | Unique row identifier |
| sensor_id | VARCHAR(32) | from `BrokerMessage.sensorId` | Which sensor detected the event |
| sensor_name | VARCHAR(128) | from `SensorSummary.name` (cached at startup) | Human-readable sensor name |
| event_type | VARCHAR(32) | from classifier output | `earthquake`, `conventional_explosion`, or `nuclear_like` |
| dominant_freq_hz | DOUBLE | from FFT analysis | The dominant frequency in Hz |
| magnitude | DOUBLE | from FFT analysis | Magnitude of the dominant component |
| detected_at | TIMESTAMPTZ | `NOW()` at insert time | When the replica classified this event |
| window_start | TIMESTAMPTZ | from `BrokerMessage.timestamp` of the first sample in the window | Start of the analysis window |
| window_end | TIMESTAMPTZ | from `BrokerMessage.timestamp` of the last sample in the window | End of the analysis window |
| region | VARCHAR(128) | from `SensorSummary.region` (cached at startup) | Geographic region of the sensor |
| category | VARCHAR(16) | from `SensorSummary.category` (cached at startup) | `field` or `datacenter` |
| replica_id | VARCHAR(32) | from environment variable `REPLICA_ID` | Which replica detected this event |

### Deduplication strategy

The unique constraint `UNIQUE (sensor_id, event_type, window_start)` ensures that if multiple replicas detect the same event from the same sensor in the same time window, only the first INSERT succeeds. All subsequent inserts are silently ignored.

**INSERT statement:**

```sql
INSERT INTO detected_events
    (sensor_id, sensor_name, event_type, dominant_freq_hz, magnitude,
     window_start, window_end, region, category, replica_id)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
ON CONFLICT (sensor_id, event_type, window_start) DO NOTHING;
```

`ON CONFLICT DO NOTHING` means: if the unique constraint is violated (another replica already inserted this event), do nothing — no error, no update, just skip. This is the simplest and safest deduplication approach.

---

## 8. Replica → Dashboard: REST API and real-time push

**Protocol:** HTTP REST + WebSocket  
**Base URL:** `http://replica:8082/api/` (accessed through Nginx gateway at port 80)  
**Direction:** Dashboard calls the replica (via Nginx round-robin)  
**Purpose:** Serve detected events for display

### 8.1 `GET /api/events` — historical event list

Query parameters (all optional):

| Parameter | Type | Example | Description |
|---|---|---|---|
| sensorId | string | `sensor-03` | Filter by sensor |
| eventType | string | `earthquake` | Filter by event type |
| region | string | `Replica Datacenter` | Filter by region |
| from | ISO-8601 | `2026-03-28T00:00:00Z` | Events detected after this time |
| to | ISO-8601 | `2026-03-28T23:59:59Z` | Events detected before this time |
| page | int | `0` | Page number (0-indexed) |
| size | int | `20` | Page size |

### Response: `EventResponse[]`

```json
[
  {
    "id": 42,
    "sensorId": "sensor-03",
    "sensorName": "Coastal Station Alpha",
    "eventType": "earthquake",
    "dominantFreqHz": 1.72,
    "magnitude": 14.3,
    "detectedAt": "2026-03-28T14:05:12.000000+00:00",
    "windowStart": "2026-03-28T14:04:58.600000+00:00",
    "windowEnd": "2026-03-28T14:05:05.000000+00:00",
    "region": "Mediterranean Coast",
    "category": "field",
    "replicaId": "replica-1"
  }
]
```

| Field | Type | Description |
|---|---|---|
| id | int | Database row ID |
| sensorId | string | Sensor identifier |
| sensorName | string | Human-readable sensor name |
| eventType | string | `earthquake`, `conventional_explosion`, or `nuclear_like` |
| dominantFreqHz | float | Dominant frequency from FFT |
| magnitude | float | FFT magnitude |
| detectedAt | string | ISO-8601 UTC timestamp of classification |
| windowStart | string | ISO-8601 start of the FFT analysis window |
| windowEnd | string | ISO-8601 end of the FFT analysis window |
| region | string | Geographic region |
| category | string | `field` or `datacenter` |
| replicaId | string | Which replica detected this |

### 8.2 `GET /api/events/{id}` — single event detail

Returns a single `EventResponse` by database ID. Returns `404` if not found.

### 8.3 `WS /api/events/ws` — real-time event push

WebSocket endpoint for real-time dashboard updates. When a replica classifies a new event and successfully persists it, it pushes the `EventResponse` to all connected WebSocket clients.

Message format: same `EventResponse` JSON as the REST endpoint.

### 8.4 `GET /api/sensors` — sensor list

Returns the cached list of `SensorSummary` objects (obtained from the simulator at startup via the broker or directly). This allows the dashboard to display sensor metadata without querying the simulator directly.

### 8.5 `GET /health` — replica health check

Used by Nginx to determine if the replica is alive.

```json
{
  "status": "UP",
  "replicaId": "replica-1",
  "uptimeSeconds": 124,
  "connectedToBroker": true,
  "connectedToControl": true
}
```

---

## 9. Nginx → Replicas: gateway routing

**Protocol:** HTTP reverse proxy  
**Port:** 80 (exposed to dashboard)  
**Purpose:** Single entry point that routes to healthy replicas

Nginx does not transform any data. It forwards requests to the upstream replica pool and returns the response unchanged. Its only logic is:

- Round-robin across alive replicas
- If a replica fails to respond (connection refused, timeout, 502/503), automatically retry on the next replica (`proxy_next_upstream`)
- Mark a replica as temporarily down after 2 consecutive failures (`max_fails=2 fail_timeout=5s`)
- Forward WebSocket upgrade headers for `/api/events/ws`

No new DTOs are introduced at this layer.

---

## 10. Simulator configuration reference

The simulator behavior can be tuned via environment variables (from `DOCKER_CONTRACT.md`):

| Variable | Type | Default | Effect |
|---|---|---|---|
| `SAMPLING_RATE_HZ` | float | `20.0` | Samples per second per sensor. Affects FFT frequency resolution. |
| `AUTO_SHUTDOWN_ENABLED` | bool | `true` | If `true`, simulator randomly emits `SHUTDOWN` commands. |
| `AUTO_SHUTDOWN_MIN_SECONDS` | float | `30.0` | Minimum delay before an automatic shutdown. |
| `AUTO_SHUTDOWN_MAX_SECONDS` | float | `90.0` | Maximum delay before an automatic shutdown. |

For development, you can disable automatic shutdowns to test the pipeline in peace:

```bash
docker run --rm -p 8080:8080 \
  -e AUTO_SHUTDOWN_ENABLED=false \
  seismic-signal-simulator:multiarch_v1
```

---

## 11. Classification rule model

### Frequency band thresholds

| Event type | Frequency range | eventType value |
|---|---|---|
| Earthquake | 0.5 Hz ≤ f < 3.0 Hz | `earthquake` |
| Conventional explosion | 3.0 Hz ≤ f < 8.0 Hz | `conventional_explosion` |
| Nuclear-like event | f ≥ 8.0 Hz | `nuclear_like` |
| Background noise | f < 0.5 Hz | not persisted |

### Detection conditions

An event is persisted only when BOTH conditions are met:

1. The dominant frequency falls within one of the classification bands (f ≥ 0.5 Hz)
2. The magnitude of the dominant frequency component exceeds a configurable noise threshold

If either condition fails, the analysis window is silently discarded and processing continues with the next window.

### FFT parameters

| Parameter | Value | Rationale |
|---|---|---|
| Window size | 128 samples | Power of 2 (required by FFT). At 20 Hz = 6.4 seconds of signal. |
| Frequency resolution | 0.15625 Hz | `20.0 / 128`. Fine enough to distinguish earthquake (0.5-3 Hz) from explosion (3-8 Hz). |
| Nyquist frequency | 10.0 Hz | `20.0 / 2`. The maximum frequency we can detect. Nuclear-like events (≥8 Hz) are within range. |
| Window overlap | implementation choice | Overlapping windows (e.g. 50%) give more frequent classifications. Non-overlapping (step = 128) is simpler. |

---

## 12. Summary of all DTOs by hop

| Hop | From → To | Protocol | DTO name | Key fields |
|---|---|---|---|---|
| 1 | Simulator → Broker | HTTP GET | `SensorSummary[]` | id, name, category, region, coordinates, websocket_url |
| 2 | Simulator → Broker | WebSocket | `SensorMeasurement` | timestamp, value |
| 3 | Broker → Replica | WebSocket | `BrokerMessage` | **sensorId**, timestamp, value |
| 4 | Simulator → Replica | SSE | `ShutdownCommand` | command: "SHUTDOWN" |
| 5 | Replica → PostgreSQL | JDBC | `DetectedEvent` (row) | sensor_id, event_type, dominant_freq_hz, magnitude, window_start, window_end |
| 6 | Replica → Dashboard | HTTP / WS | `EventResponse` | id, sensorId, sensorName, eventType, dominantFreqHz, magnitude, detectedAt, region |

The bold `sensorId` in hop 3 marks the only field that our code adds. Everything else is either passed through from the simulator or computed by the FFT/classifier.
