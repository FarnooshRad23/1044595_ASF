package com.advprog.processing.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.advprog.processing.service.SlidingWindowService.WindowResult;

@Service
public class ClassificationService {
    private static final Logger log = LoggerFactory.getLogger(ClassificationService.class);

    private final FftAnalysisService fftAnalysisService;

    public ClassificationService(FftAnalysisService fftAnalysisService) {
        this.fftAnalysisService = fftAnalysisService;
    }

    /**
     * Classifies a completed 128-sample window by dominant frequency.
     * Returns null when the dominant frequency is below NOISE_THRESHOLD Hz.
     * The caller MUST check for null and skip persistence entirely.
     *
     * @param result         the completed window
     * @param samplingRateHz the sensor's sampling rate
     * @return event type string, or null if frequency is below NOISE_THRESHOLD
     */
    public String classify(WindowResult result, double samplingRateHz) {
        double dominantFreq = fftAnalysisService.dominantFrequencyHz(result.samples(), samplingRateHz);

        double NOISE_THRESHOLD = 0.5;
        if (dominantFreq < NOISE_THRESHOLD) {
            log.debug("Sensor {} below noise threshold ({} Hz) — discarding window", result.sensorId(), dominantFreq);
            return null;
        }

        log.info("Sensor {} dominant frequency: {} Hz", result.sensorId(), dominantFreq);

        if (dominantFreq < 3.0) return "earthquake";
        if (dominantFreq < 8.0) return "conventional_explosion";
        return "nuclear_like";
    }
}
