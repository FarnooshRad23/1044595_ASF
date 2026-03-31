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