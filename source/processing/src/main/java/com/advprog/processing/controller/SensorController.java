package com.advprog.processing.controller;

import com.advprog.processing.dto.SensorSummary;
import com.advprog.processing.service.SensorCacheService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
public class SensorController {

    private final SensorCacheService sensorCacheService;

    public SensorController(SensorCacheService sensorCacheService) {
        this.sensorCacheService = sensorCacheService;
    }

    @GetMapping("/api/sensors")
    public Collection<SensorSummary> getSensors() {
        return sensorCacheService.getAll().values();
    }
}
