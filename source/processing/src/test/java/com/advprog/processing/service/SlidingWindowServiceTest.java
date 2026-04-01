package com.advprog.processing.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SlidingWindowServiceTest {

    private SlidingWindowService service;

    @BeforeEach
    void setUp() {
        service = new SlidingWindowService();
    }

    @Test
    void first127SamplesReturnEmpty() {
        for (int i = 1; i < SlidingWindowService.WINDOW_SIZE; i++) {
            Optional<SlidingWindowService.WindowResult> result =
                    service.addSample("sensor-01", i * 1.0, "t" + i);
            assertTrue(result.isEmpty(), "Expected empty at sample " + i);
        }
    }

    @Test
    void on128thSampleReturnsWindowResult() {
        for (int i = 1; i < SlidingWindowService.WINDOW_SIZE; i++) {
            service.addSample("sensor-01", i * 1.0, "t" + i);
        }
        Optional<SlidingWindowService.WindowResult> result =
                service.addSample("sensor-01", 128.0, "t128");

        assertTrue(result.isPresent());
        SlidingWindowService.WindowResult w = result.get();
        assertEquals("sensor-01", w.sensorId());
        assertEquals(128, w.samples().length);
        assertEquals("t1", w.windowStart());
        assertEquals("t128", w.windowEnd());
        assertEquals(1.0, w.samples()[0]);
        assertEquals(128.0, w.samples()[127]);
    }

    @Test
    void afterDrainNextWindowResetsCleanly() {
        // Fill first window
        for (int i = 1; i <= SlidingWindowService.WINDOW_SIZE; i++) {
            service.addSample("sensor-01", i * 1.0, "t" + i);
        }

        // Fill second window
        Optional<SlidingWindowService.WindowResult> second = Optional.empty();
        for (int i = 1; i <= SlidingWindowService.WINDOW_SIZE; i++) {
            second = service.addSample("sensor-01", i * 2.0, "t2_" + i);
        }

        assertTrue(second.isPresent());
        SlidingWindowService.WindowResult w = second.get();
        assertEquals(128, w.samples().length);
        assertEquals("t2_1", w.windowStart());
        assertEquals("t2_128", w.windowEnd());
        assertEquals(2.0, w.samples()[0]);
    }

    @Test
    void twoSensorsHaveIndependentBuffers() {
        // Add 127 samples to sensor-A
        for (int i = 1; i < SlidingWindowService.WINDOW_SIZE; i++) {
            service.addSample("sensor-A", i * 1.0, "tA" + i);
        }

        // Add 128 samples to sensor-B — should complete independently
        Optional<SlidingWindowService.WindowResult> resultB = Optional.empty();
        for (int i = 1; i <= SlidingWindowService.WINDOW_SIZE; i++) {
            resultB = service.addSample("sensor-B", i * 1.0, "tB" + i);
        }

        assertTrue(resultB.isPresent());
        assertEquals("sensor-B", resultB.get().sensorId());

        // sensor-A's 128th sample should now complete its window
        Optional<SlidingWindowService.WindowResult> resultA =
                service.addSample("sensor-A", 128.0, "tA128");
        assertTrue(resultA.isPresent());
        assertEquals("sensor-A", resultA.get().sensorId());
        assertEquals("tA1", resultA.get().windowStart());
    }
}
