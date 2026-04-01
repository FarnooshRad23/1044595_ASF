HEAD
package com.advprog.processing.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class EventPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(EventPersistenceService.class);

    private static final String INSERT_SQL = """
            INSERT INTO detected_events
                (sensor_id, sensor_name, event_type, dominant_freq_hz, magnitude,
                 window_start, window_end, region, category, replica_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (sensor_id, event_type, window_start) DO NOTHING
            """;

    private final JdbcTemplate jdbc;

    @Value("${REPLICA_ID:replica-1}")
    private String replicaId;

    public EventPersistenceService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Persists a detected event.
     *
     * @return true if the row was inserted, false if it was a duplicate (ON CONFLICT DO NOTHING)
     */
    public boolean persist(String sensorId, String sensorName, String eventType,
                           double dominantFreqHz, double magnitude,
                           String windowStart, String windowEnd,
                           String region, String category) {
        int rows = jdbc.update(INSERT_SQL,
                sensorId, sensorName, eventType, dominantFreqHz, magnitude,
                OffsetDateTime.parse(windowStart), OffsetDateTime.parse(windowEnd),
                region, category, replicaId);
        if (rows > 0) {
            log.info("Persisted: sensor={} type={} freq={}Hz replica={}", sensorId, eventType, dominantFreqHz, replicaId);
        } else {
            log.debug("Duplicate skipped: sensor={} type={} windowStart={}", sensorId, eventType, windowStart);
        }
        return rows > 0;
    }
}

package com.advprog.processing.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.advprog.processing.dto.SensorSummary;

@Service
public class EventPersistenceService {

    private static final Logger log =
            LoggerFactory.getLogger(EventPersistenceService.class);

    public void save(String sensorID,
                     SensorSummary sensor,
                     String classification,
                     double frequency,
                     double magnitude,
                     String windowStart,
                     String windowEnd) {

        log.info("Persisting event: sensorId={} name={} classification={} freq={}Hz magnitude={} windowStart={} windowEnd={}",
                sensorID,
                sensor != null ? sensor.name() : "unknown",
                classification,
                frequency,
                magnitude,
                windowStart,
                windowEnd);
    }
}
old-private-repo/processing-classification
