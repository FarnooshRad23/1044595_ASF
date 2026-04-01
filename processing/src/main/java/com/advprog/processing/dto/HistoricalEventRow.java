package com.advprog.processing.dto;

public record HistoricalEventRow(
        String eventId,
        String sensorId,
        String region,
        String time,    // ISO-8601 (UTC o offset)
        String type,
        double freq,
        double amp
) {}