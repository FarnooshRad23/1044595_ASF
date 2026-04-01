package com.advprog.processing.controller;

import com.advprog.processing.dto.HealthResponse;
import com.advprog.processing.service.BrokerClientService;
import com.advprog.processing.service.ControlStreamService;
import com.advprog.processing.service.SensorCacheService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
public class HealthController {

    private final Instant startTime = Instant.now();

    private final SensorCacheService sensorCacheService;
    private final BrokerClientService brokerClientService;
    private final ControlStreamService controlStreamService;
    private final String replicaId;

    public HealthController(SensorCacheService sensorCacheService,
                            BrokerClientService brokerClientService,
                            ControlStreamService controlStreamService,
                            @Value("${REPLICA_ID:replica-1}") String replicaId) {
        this.sensorCacheService = sensorCacheService;
        this.brokerClientService = brokerClientService;
        this.controlStreamService = controlStreamService;
        this.replicaId = replicaId;
    }

<<<<<<< HEAD
    @GetMapping("/api/health")
=======
    @GetMapping("/health")
>>>>>>> old-private-repo/frontendtobackend
    public HealthResponse health() {
        boolean broker = brokerClientService.isBrokerConnected();
        boolean control = controlStreamService.isControlConnected();
        String status = (broker && control) ? "up" : "degraded";
        long uptime = Instant.now().getEpochSecond() - startTime.getEpochSecond();
        return new HealthResponse(status, replicaId, sensorCacheService.getAll().size(),
                uptime, broker, control);
    }
}
