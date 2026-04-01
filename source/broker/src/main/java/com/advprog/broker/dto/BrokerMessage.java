package com.advprog.broker.dto;

public record BrokerMessage(String sensorId, String timestamp, double value) {}
