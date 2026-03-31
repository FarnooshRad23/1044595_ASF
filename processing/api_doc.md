# Processing Replica — API Reference

Base URL (via Nginx): `http://localhost:80`
Direct replica URL (dev only): `http://localhost:8082`

> The dashboard must **always** talk to Nginx (`/api/…`), never to a replica directly.
> Nginx load-balances across `processing-1` and `processing-2` on port 8082.

---

## Endpoints

### GET /health

Returns the current liveness state of this replica. Nginx uses this to decide whether to route traffic here.

**Response** `200 OK` — always responds, even when degraded.

```json
{
  "status": "up",
  "replicaId": "replica-1",
  "totalSensors": 12,
  "uptimeSeconds": 142,
  "brokerConnected": true,
  "controlConnected": true
}
```

| Field | Type | Description |
|-------|------|-------------|
| `status` | `"up"` \| `"degraded"` | `"up"` only when both broker WS and simulator SSE connections are live |
| `replicaId` | string | Which replica answered this request (`replica-1` or `replica-2`) |
| `totalSensors` | number | Number of sensors currently in this replica's cache |
| `uptimeSeconds` | number | Seconds since this replica started |
| `brokerConnected` | boolean | Is the WebSocket connection to the broker alive? |
| `controlConnected` | boolean | Is the SSE connection to the simulator control stream alive? |

**Dashboard use:** poll every 5–10 s to show a "replica status" indicator.

---

### GET /api/sensors

Returns the list of all known sensors, as fetched from the simulator at startup.

**Response** `200 OK`

```json
[
  {
    "id": "sensor-01",
    "name": "Mount Etna Station A",
    "category": "volcanic",
    "region": "Sicily",
    "coordinates": {
      "latitude": 37.748,
      "longitude": 14.999
    },
    "measurement_unit": "m/s²",
    "sampling_rate_hz": 100.0,
    "websocket_url": "ws://simulator:8080/ws/sensor-01"
  }
]
```

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Unique sensor identifier (used as key in events) |
| `name` | string | Human-readable station name |
| `category` | string | Sensor category (e.g. `"volcanic"`, `"tectonic"`) |
| `region` | string | Geographic region |
| `coordinates.latitude` | number | Latitude |
| `coordinates.longitude` | number | Longitude |
| `measurement_unit` | string | Physical unit of the raw signal |
| `sampling_rate_hz` | number | Samples per second — used to compute dominant frequency |
| `websocket_url` | string | Internal WS URL (broker-facing, not needed by dashboard) |

**Dashboard use:** fetch once on load to build a sensor name/region lookup table for decorating event rows.

---

### GET /api/events

Returns the list of detected seismic events persisted in the database, most recent first.

> **Status: not yet implemented** — will be available once `EventPersistenceService` and `EventController` are wired up.

**Response** `200 OK`

```json
[
  {
    "id": 1042,
    "sensorId": "sensor-01",
    "sensorName": "Mount Etna Station A",
    "eventType": "earthquake",
    "dominantFreqHz": 1.56,
    "magnitude": 0.83,
    "detectedAt": "2026-03-31T14:22:05.123Z",
    "region": "Sicily",
    "category": "volcanic",
    "replicaId": "replica-1"
  }
]
```

| Field | Type | Description |
|-------|------|-------------|
| `id` | number | Auto-incremented DB primary key |
| `sensorId` | string | Source sensor identifier |
| `sensorName` | string | Human-readable sensor name (joined from sensor cache) |
| `eventType` | string | `"earthquake"`, `"conventional_explosion"`, or `"nuclear_like"` |
| `dominantFreqHz` | number | Dominant frequency from FFT analysis (Hz) |
| `magnitude` | number | Signal magnitude (dimensionless, derived from FFT peak) |
| `detectedAt` | ISO 8601 string | When this event was detected (UTC) |
| `region` | string | Region of the source sensor |
| `category` | string | Category of the source sensor |
| `replicaId` | string | Which replica detected and persisted this event |

**Event type classification thresholds:**

| Dominant frequency | Event type |
|--------------------|------------|
| < 0.5 Hz | discarded (noise — never stored) |
| 0.5 – 3 Hz | `earthquake` |
| 3 – 8 Hz | `conventional_explosion` |
| ≥ 8 Hz | `nuclear_like` |

**Dashboard use:** poll this endpoint on a short interval (e.g. every 2 s) to refresh the event list, OR subscribe via WebSocket (see below) to receive events in real time without polling.

---

### WebSocket /api/events/ws

Real-time push stream of detected events. The server sends a new JSON message every time a processing window completes and produces a classified event.

> **Status: not yet implemented** — same milestone as `GET /api/events`.

**Connection:** `ws://localhost:80/api/events/ws`

**Message format** — same shape as a single item from `GET /api/events`:

```json
{
  "id": 1043,
  "sensorId": "sensor-03",
  "sensorName": "Apennine Ridge B",
  "eventType": "conventional_explosion",
  "dominantFreqHz": 5.12,
  "magnitude": 1.20,
  "detectedAt": "2026-03-31T14:22:07.456Z",
  "region": "Central Italy",
  "category": "tectonic",
  "replicaId": "replica-2"
}
```

The server never sends a message for noise windows (dominant frequency < 0.5 Hz).

**Dashboard use:** preferred approach for the live feed. Connect on page load, append each incoming message to the top of the event table. Keep at most N rows in the UI to avoid memory growth.

---

## Quick-start for the dashboard

```ts
// 1. Load sensors once for name/region lookup
const sensors = await fetch('/api/sensors').then(r => r.json());
const sensorMap = Object.fromEntries(sensors.map(s => [s.id, s]));

// 2. Subscribe to live events
const ws = new WebSocket('ws://localhost:80/api/events/ws');
ws.onmessage = (e) => {
  const event = JSON.parse(e.data);
  // prepend event to table, trim old rows
};

// 3. Load historical events on mount (initial fill of the table)
const history = await fetch('/api/events').then(r => r.json());

// 4. Health check (optional — for status badge)
const health = await fetch('/health').then(r => r.json());
// health.status === "up" | "degraded"
```

---

## Error responses

All endpoints return standard Spring Boot error JSON on failure:

```json
{
  "timestamp": "2026-03-31T14:00:00.000+00:00",
  "status": 500,
  "error": "Internal Server Error",
  "path": "/api/events"
}
```
