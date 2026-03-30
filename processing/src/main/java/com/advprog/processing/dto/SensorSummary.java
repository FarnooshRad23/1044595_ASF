package com.advprog.processing.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Mirrors the JSON object returned by GET /api/devices/ on the simulator.
 * @JsonProperty is only needed where the JSON key differs from the component name.
 */
public record SensorSummary(
        String id,
        String name,
        String category,
        String region,
        Coordinates coordinates,
        @JsonProperty("measurement_unit") String measurementUnit,
        @JsonProperty("sampling_rate_hz")  double samplingRateHz,
        @JsonProperty("websocket_url")     String websocketUrl
) {}
