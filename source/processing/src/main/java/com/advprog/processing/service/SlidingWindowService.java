package com.advprog.processing.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SlidingWindowService {

    static final int WINDOW_SIZE = 128;

    public record WindowResult(String sensorId, double[] samples,
                               String windowStart, String windowEnd) {}

    private static class WindowBuffer {
        final List<Double> values = new ArrayList<>(WINDOW_SIZE);
        String firstTimestamp;
        String lastTimestamp;
    }

    private final ConcurrentHashMap<String, WindowBuffer> buffers = new ConcurrentHashMap<>();

    public Optional<WindowResult> addSample(String sensorId, double value, String timestamp) {
        WindowBuffer buf = buffers.computeIfAbsent(sensorId, id -> new WindowBuffer());

        synchronized (buf) {
            if (buf.values.isEmpty()) {
                buf.firstTimestamp = timestamp;
            }
            buf.values.add(value);
            buf.lastTimestamp = timestamp;

            if (buf.values.size() == WINDOW_SIZE) {
                double[] samples = buf.values.stream().mapToDouble(Double::doubleValue).toArray();
                String start = buf.firstTimestamp;
                String end = buf.lastTimestamp;

                buf.values.clear();
                buf.firstTimestamp = null;
                buf.lastTimestamp = null;

                return Optional.of(new WindowResult(sensorId, samples, start, end));
            }
        }
        return Optional.empty();
    }
}
