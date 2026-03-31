CREATE TABLE IF NOT EXISTS detected_events (
    id               BIGSERIAL PRIMARY KEY,
    sensor_id        TEXT NOT NULL,
    sensor_name      TEXT,
    event_type       TEXT NOT NULL,
    dominant_freq_hz DOUBLE PRECISION NOT NULL,
    magnitude        DOUBLE PRECISION NOT NULL,
    window_start     TIMESTAMPTZ NOT NULL,
    window_end       TIMESTAMPTZ NOT NULL,
    region           TEXT,
    category         TEXT,
    replica_id       TEXT NOT NULL,
    detected_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (sensor_id, event_type, window_start)
);
