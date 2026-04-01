package com.advprog.broker.dto;

public record SensorSummary(
    String id,
    String name,
    String category,
    String region,
    Coordinates coordinates,
    String measurement_unit,
    double sampling_rate_hz,
    String websocket_url
) {}
