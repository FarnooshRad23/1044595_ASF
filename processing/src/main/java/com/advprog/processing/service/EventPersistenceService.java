<<<<<<< HEAD
HEAD
package com.advprog.processing.service;

=======
package com.advprog.processing.service;

import java.time.Duration;
import java.time.OffsetDateTime;

>>>>>>> old-private-repo/frontendtobackend
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
<<<<<<< HEAD
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
=======
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import com.advprog.processing.dto.Coordinates;
import com.advprog.processing.dto.Event;
>>>>>>> old-private-repo/frontendtobackend

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
<<<<<<< HEAD
=======
    private final EventDeliveryService delivery;
>>>>>>> old-private-repo/frontendtobackend

    @Value("${REPLICA_ID:replica-1}")
    private String replicaId;

<<<<<<< HEAD
    public EventPersistenceService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

=======
    public EventPersistenceService(JdbcTemplate jdbc,EventDeliveryService delivery) {
       this.jdbc = jdbc;
       this.delivery = delivery;
    }
 
>>>>>>> old-private-repo/frontendtobackend
    /**
     * Persists a detected event.
     *
     * @return true if the row was inserted, false if it was a duplicate (ON CONFLICT DO NOTHING)
     */
    public boolean persist(String sensorId, String sensorName, String eventType,
                           double dominantFreqHz, double magnitude,
                           String windowStart, String windowEnd,
<<<<<<< HEAD
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
=======
                           String region, String category,Coordinates coordinates,String detectedAt) {
                             
        KeyHolder keyHolder = new GeneratedKeyHolder();

jdbc.update(connection -> {
    var ps = connection.prepareStatement(INSERT_SQL, new String[]{"id"});
    ps.setString(1, sensorId);
    ps.setString(2, sensorName);
    ps.setString(3, eventType);
    ps.setDouble(4, dominantFreqHz);
    ps.setDouble(5, magnitude);
    ps.setObject(6, OffsetDateTime.parse(windowStart));
    ps.setObject(7, OffsetDateTime.parse(windowEnd));
    ps.setString(8, region);
    ps.setString(9, category);
    ps.setString(10, replicaId);
    return ps;
}, keyHolder);

Number key = keyHolder.getKey();
        if (key != null) {
            log.info("Persisted: sensor={} type={} freq={}Hz replica={}", sensorId, eventType, dominantFreqHz, replicaId);
               String id=""+key;
               OffsetDateTime start = OffsetDateTime.parse(windowStart);
               OffsetDateTime end = OffsetDateTime.parse(windowEnd);
               double duration = Duration.between(start,end).toMillis() / 1000.0;
                Event event = new Event(  //I parametri di event sono scelti in coesione con quelli della dasboard
                    id,
                    eventType,
                    dominantFreqHz,
                    magnitude,
                    duration,
                    windowStart, 
                    windowEnd,
                    region, 
                    category
            );
      delivery.publish(event);    
        } else {
            log.debug("Duplicate skipped: sensor={} type={} windowStart={}", sensorId, eventType, windowStart);
        }
        return key !=null;
    }
}
>>>>>>> old-private-repo/frontendtobackend
