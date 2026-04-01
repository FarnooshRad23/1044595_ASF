# Sensor Metadata Handling Strategy

## Problem Overview

Each processing replica must enrich incoming seismic events with sensor metadata, specifically:
- Sensor name
- Region
- Category

However, replicas operate in a **fault-tolerant distributed environment** where:
- Replicas may be terminated at any time
- New replicas may start dynamically
- The broker is responsible only for data distribution

> How should replicas access and manage sensor metadata efficiently and reliably?

---

## Considered Approaches

### 1. Replica Fetch + Cache (Decentralized)

Each replica:
- Fetches sensor metadata from the `/api/devices` endpoint
- Stores it locally in an in-memory map
- Uses it to enrich events during processing
- Optionally performs lazy fetch for missing entries

---

### 2. Broker Forwards Metadata (Centralized)

The broker:
- Fetches sensor metadata
- Sends metadata to replicas upon connection (e.g., via WebSocket)
- Replicas rely on the broker for enrichment data

---

## Chosen Approach: Replica Fetch + Cache

We adopt the **Replica Fetch + Cache** strategy.

### How it works

- At startup, each replica retrieves all sensor metadata and builds an in-memory cache: `Map<SensorId, SensorMetadata>`
- During event processing, metadata is retrieved via constant-time lookup
- If metadata is missing, it is fetched on demand and added to the cache

---

## Advantages

### ✔ Fault Tolerance
Each replica is fully autonomous:
- Can start, stop, and recover independently
- Does not depend on other components for metadata

### ✔ Performance
- Metadata is stored locally → no runtime network calls
- Fast O(1) lookups during event processing

### ✔ Simplicity
- No need for synchronization between components
- No additional infrastructure required

### ✔ Separation of Concerns
- Broker remains a pure data distributor
- Replicas handle enrichment and processing logic

---

## Limitations of Broker-Based Approach

### ✖ Violates Architectural Constraints
The broker is intended to handle **only data distribution**, not data enrichment or metadata management.

### ✖ Increased Coupling
- Replicas depend on broker behavior and message format
- Changes in broker logic affect all replicas

### ✖ Reduced Fault Tolerance
- Replicas cannot operate correctly without metadata from the broker
- Requires complex synchronization or replay mechanisms

### ✖ Additional Complexity
- Broker must manage metadata state and delivery
- Introduces coordination challenges in case of failures or restarts

---

## Conclusion

The **Replica Fetch + Cache** approach provides a simple, robust, and scalable solution aligned with distributed system principles.

It ensures that:
- Replicas remain independent and resilient
- The broker maintains a clear and limited responsibility
- The system avoids unnecessary complexity while meeting all requirements

Optional improvements such as periodic cache refresh or monitoring can be added without altering the core design.
