CREATE TABLE IF NOT EXISTS detected_events (
    id               BIGSERIAL PRIMARY KEY,
    sensor_id        TEXT NOT NULL,
    sensor_name      TEXT,
    event_type       TEXT NOT NULL,
    dominant_freq_hz DOUBLE PRECISION NOT NULL,
    magnitude        DOUBLE PRECISION NOT NULL,
    window_start     TIMESTAMP WITH TIME ZONE NOT NULL,
    window_end       TIMESTAMP WITH TIME ZONE NOT NULL,
    region           TEXT,
    category         TEXT,
    replica_id       TEXT NOT NULL,
    detected_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    UNIQUE (sensor_id, event_type, window_start)
);

CREATE INDEX IF NOT EXISTS idx_events_detected_at ON detected_events (detected_at DESC);
CREATE INDEX IF NOT EXISTS idx_events_sensor      ON detected_events (sensor_id);
CREATE INDEX IF NOT EXISTS idx_events_type        ON detected_events (event_type);