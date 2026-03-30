# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Aegis** — a real-time seismic signal processing and threat detection platform (Hackathon exam, Sapienza MSc ACSAI, deadline March 31 2026). Sensors feed a custom broker that fans out to replicated processing services performing FFT-based frequency classification, persisting events to a shared DB, and streaming results to a live dashboard.

## Exam Baseline Requirements (must ALL be satisfied to pass)

1. **Distributed architecture** — no single monolithic service
2. **Fault tolerance** — replica failure must not interrupt overall operation
3. **Replicated processing services** — multiple replicas; only replicas are subject to failure
4. **Sliding-window processing + DFT/FFT** — 128-sample window per sensor, Apache Commons Math
5. **Event classification** — based on dominant frequency (see thresholds below)
6. **Duplicate-safe persistent event storage** — shared external DB (Postgres/MongoDB, no SQLite/embedded)
7. **Real-time dashboard** — live event feed via WebSocket/SSE/polling + historical inspection
8. **Docker reproducibility** — `docker compose up` starts everything; no manual setup

## Required Deliverables (repo must contain)

```
.
├── input.md          # system overview, user stories, event schema, rule model
├── Student_doc.md    # specifics of the deployed system
├── source/           # all source code, Dockerfiles, docker-compose.yml
└── booklets/         # slides, diagrams, any supporting docs
```

- **Dockerfile per service** + **single `docker-compose.yml`** that starts everything including simulator
- **`input.md`**: user stories (15 for 2-person team), each with LoFi mockup + NFR notes
- **`Student_doc.md`**: deployed system specifics
- **Architecture diagrams** (already exist in `booklets/`)
- **Presentation slides**

## Components & Ports

| Component | Technology | Port | Status |
|-----------|-----------|------|--------|
| **Simulator** | Docker image | 8080 | Pre-built (OCI tar) |
| **Broker** | Java 17 / Spring Boot 4.0.5 | 8081 | In development (`feat/broker`) |
| **Replica** | Java 17 / Spring Boot (expected) | 8082 | Not yet created |
| **Dashboard** | React 19 + TypeScript + Vite | 5173 | Scaffolded |
| **PostgreSQL** | Postgres | 5432 | External |
| **Nginx** | Reverse proxy | 80 | External |

## Commands

### Simulator
```bash
cd simulator/
docker load -i seismic-signal-simulator-oci.tar
docker run --rm -p 8080:8080 seismic-signal-simulator:multiarch_v1

# Disable auto-shutdowns during development:
docker run --rm -p 8080:8080 -e AUTO_SHUTDOWN_ENABLED=false seismic-signal-simulator:multiarch_v1
```

### Broker
```bash
cd broker/
./mvnw spring-boot:run     # Run
./mvnw clean install       # Build
./mvnw test                # Run tests
```

### Dashboard
```bash
cd dashboard/
npm run dev      # Dev server on :5173
npm run build    # Production build
npm run lint     # ESLint
```

### Manual Testing
```bash
# Verify broker broadcasts BrokerMessage JSON to replicas
websocat ws://localhost:8081/stream | head -5
# Expected: {"sensorId":"sensor-XX","timestamp":"...","value":...}

# Verify simulator sensor list
curl http://localhost:8080/api/devices/ | jq .
```

## Architecture & Data Flow

```
Simulator ──[SensorMeasurement]──▶ Broker ──[BrokerMessage]──▶ Replica(s)
                │                                                    │
                └──[ShutdownCommand via SSE]──────────────────────▶  │
                                                              Sliding window (128 samples)
                                                              FFT → dominant frequency
                                                              Classification
                                                                    │
                                                           [DetectedEvent] ──▶ PostgreSQL
                                                                                    │
                                                                       [EventResponse] ──▶ Dashboard
```

**The broker's only job:** enrich `SensorMeasurement` (no `sensorId`) with the sensor ID (known from which WS connection received the message) to produce `BrokerMessage`, then broadcast to all replica sessions. No FFT, no classification, no DB access.

## Key DTOs

| Hop | DTO | Fields |
|-----|-----|--------|
| Simulator → Broker (HTTP) | `SensorSummary[]` | id, name, category, region, coordinates, measurement_unit, sampling_rate_hz, websocket_url |
| Simulator → Broker (WS) | `SensorMeasurement` | timestamp, value |
| Broker → Replicas (WS) | `BrokerMessage` | **sensorId** (added by broker), timestamp, value |
| Simulator → Replicas (SSE) | `ShutdownCommand` | command: "SHUTDOWN" |
| Replica → DB (JDBC) | `detected_events` table | sensor_id, event_type, dominant_freq_hz, magnitude, window_start, window_end |
| Replica → Dashboard | `EventResponse` | id, sensorId, sensorName, eventType, dominantFreqHz, magnitude, detectedAt, region, category, replicaId |

## Broker Implementation Guide (F1–F4)

The broker's 4 features (documented in `broker/README.md`):

- **F1**: `@Service` + `@PostConstruct` → `GET http://simulator:8080/api/devices/` → cache `List<SensorSummary>`
- **F2**: For each sensor, open `WS simulator:8080{sensor.websocket_url}`, track `connection → sensorId`, deserialize `SensorMeasurement`, wrap into `BrokerMessage`
- **F3**: `@Configuration @EnableWebSocket` registers `/stream` handler; store sessions in `ConcurrentHashMap.newKeySet()`
- **F4**: On each incoming `SensorMeasurement`, serialize `BrokerMessage` and `session.sendMessage()` to all replica sessions; on `IOException`, remove the dead session

Planned package structure: `com.advprog.broker.{config,dto,service,handler}`

## Replica Implementation Notes

- Maintains 12 sliding window buffers (`Map<String, Deque<BrokerMessage>>`) — one per sensor
- On every 128th sample per sensor: run FFT (`Apache Commons Math FastFourierTransformer`), find dominant bin (skip bin 0), convert to Hz: `binIndex × samplingRateHz / 128`
- Classification thresholds: f < 0.5 Hz → discard | 0.5–3 Hz → `earthquake` | 3–8 Hz → `conventional_explosion` | ≥8 Hz → `nuclear_like`
- Deduplication: `INSERT ... ON CONFLICT (sensor_id, event_type, window_start) DO NOTHING`
- Must connect independently to `GET http://simulator:8080/api/control` (SSE) and call `System.exit(0)` on `SHUTDOWN` command
- `REPLICA_ID` env var identifies which replica persisted each event
- Expose `GET /health`, `GET /api/events`, `GET /api/sensors`, `WS /api/events/ws`

## Gateway Requirements (Nginx)

The system must expose a **single entry point** between dashboard and backend:
- Route requests to available replicas (round-robin)
- Health-check replicas and exclude failed ones automatically (`max_fails=2 fail_timeout=5s`)
- Forward WebSocket upgrade headers for `/api/events/ws`

Dashboard's `vite.config.ts` proxies `/api` → `http://localhost:80` (Nginx). Dashboard never calls broker or simulator directly.

## Reference Documentation

Full contracts and schemas in `booklets/`:
- `API_CONTRACT.md` — Simulator REST/WS API spec
- `DOCKER_CONTRACT.md` — Simulator Docker env vars
- `event_schema_and_data_interfaces.md` — Complete 12-hop data pipeline with all DTOs and DB schema
- `dto_summary_cheat_sheet.html` — Quick-reference for all data structures
