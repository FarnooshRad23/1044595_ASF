package com.advprog.broker.service;

import com.advprog.broker.dto.SensorSummary;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Queries the simulator for the full list of sensors exactly once, at application startup.
 *
 * <p>The simulator exposes a REST endpoint ({@code GET /api/devices/}) that returns the
 * complete catalogue of sensors as a JSON array.
 *
 * <p>This service issues that single HTTP request during the Spring {@code @PostConstruct}
 * phase — after all beans have been wired but before the application starts serving traffic.
 */
@Service
public class SensorDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(SensorDiscoveryService.class);

    @Value("${simulator.base-url}")
    private String simulatorBaseUrl;

    private final RestTemplate restTemplate;

    public SensorDiscoveryService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // Starts empty; populated once by discoverSensors() at startup.
    private List<SensorSummary> sensors = List.of();

    /**
     * Fetches all sensors from the simulator in a single HTTP GET request.
     *
     * The call is blocking and synchronous — the broker will not
     * finish starting up until the request completes (or fails).
     *
     * <p>The request targets {@code GET {simulatorBaseUrl}/api/devices/}, which returns a
     * JSON array of {@link SensorSummary} objects. The full array is deserialized in one
     * shot and stored as an immutable list.
     *
     * <p>Fault tolerance: if the simulator is not reachable (wrong hostname, not yet started,
     * network error), the exception is caught and logged as a warning. The broker starts
     * normally with an empty sensor list. Any downstream component that depends on sensor
     * data (e.g. the WebSocket fan-out) will simply have nothing to work with until the
     * broker is restarted with the simulator available.
     */
    @PostConstruct
    public void discoverSensors() {
        String url = simulatorBaseUrl + "/api/devices/";
        try {
            // Single blocking GET — deserializes the entire JSON array at once.
            SensorSummary[] result = restTemplate.getForObject(url, SensorSummary[].class);
            sensors = (result != null) ? List.of(result) : List.of();

            log.info("Discovered {} sensors: {}",
                sensors.size(),
                sensors.stream().map(SensorSummary::id).toList());
        } catch (Exception e) {
            // Do not let a missing simulator crash the broker at startup.
            // F2 (WebSocket connections) will have nothing to connect to, but the
            // application context remains alive and can be restarted cleanly.
            log.warn("Could not reach simulator at {} — starting with empty sensor list. Cause: {}",
                url, e.getMessage());
        }
    }

    /**
     * Returns the sensor list discovered at startup. Immutable; never {@code null}.
     * Empty if the simulator was unreachable when the broker started.
     */
    public List<SensorSummary> getSensors() {
        return sensors;
    }
}
