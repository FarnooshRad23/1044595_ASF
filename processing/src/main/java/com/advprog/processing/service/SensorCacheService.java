package com.advprog.processing.service;

import com.advprog.processing.dto.SensorSummary;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SensorCacheService {

    private static final Logger log = LoggerFactory.getLogger(SensorCacheService.class);

    private final RestClient restClient;
    // One entry per sensor; written once at startup, read on every message.
    private final ConcurrentHashMap<String, SensorSummary> cache = new ConcurrentHashMap<>();

    /**
     * RestClient.Builder is a prototype bean in Spring Boot 4 — each injection site
     * gets its own instance. We pin the base URL here so callers only specify paths.
     */
    public SensorCacheService(RestClient.Builder builder,
                              @Value("${simulator.url}") String simulatorUrl) {
        this.restClient = builder.baseUrl(simulatorUrl).build();
    }

    /**
     * Runs once when the Spring context is ready. Delegates to loadCache() so tests
     * can call loadCache() directly without triggering @PostConstruct via a Spring context.
     * Wrapped in try/catch so a missing simulator does not abort the entire context startup.
     */
    @PostConstruct
    public void init() {
        try {
            loadCache();
        } catch (Exception e) {
            log.warn("SensorCacheService: could not load sensor list at startup ({}). " +
                     "Cache is empty; lazy fetch will retry on first use.", e.getMessage());
        }
    }

    /**
     * Fetches all sensors from /api/devices/ and repopulates the cache.
     * Called at startup and again on a cache miss.
     */
    public void loadCache() {
        List<SensorSummary> sensors = restClient.get()
                .uri("/api/devices/")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (sensors == null || sensors.isEmpty()) {
            log.warn("SensorCacheService: /api/devices/ returned empty list");
            return;
        }

        cache.clear();
        for (SensorSummary s : sensors) {
            cache.put(s.id(), s);
            log.info("Cached sensor: id={} name={} region={} rate={}Hz",
                    s.id(), s.name(), s.region(), s.samplingRateHz());
        }
        log.info("SensorCacheService: {} sensor(s) loaded", cache.size());
    }

    /**
     * Returns the cached entry. On a miss, re-fetches the full device list once
     * (no single-sensor endpoint exists on the simulator).
     */
    public Optional<SensorSummary> get(String sensorId) {
        SensorSummary hit = cache.get(sensorId);
        if (hit != null) return Optional.of(hit);

        log.info("Cache miss for sensorId={}; re-fetching device list", sensorId);
        loadCache();
        return Optional.ofNullable(cache.get(sensorId));
    }

    /** Returns the live cache map. Used by the future /api/sensors REST endpoint. */
    public ConcurrentHashMap<String, SensorSummary> getAll() {
        return cache;
    }
}
