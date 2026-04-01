package com.advprog.processing.dto;

public record Event (String id,
 String eventType,
                           double dominantFreqHz, double magnitude,
                           double duration,
                           String windowStart, String windowEnd,
                           String region, String category) {}