package com.advprog.processing.dto;

public record HealthResponse(
        String status,
        String replicaId,
        int totalSensors,
        long uptimeSeconds,
        boolean brokerConnected,
        boolean controlConnected
) {}
