HEAD
package com.advprog.processing.service;

import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ClassificationService {

    public Optional<String> classify(double dominantFreqHz) {
        if (dominantFreqHz < 0.5) {
            return Optional.empty();
        } else if (dominantFreqHz < 3.0) {
            return Optional.of("earthquake");
        } else if (dominantFreqHz < 8.0) {
            return Optional.of("conventional_explosion");
        } else {
            return Optional.of("nuclear_like");
        }
    }
}

package com.advprog.processing.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ClassificationService {
    private static final Logger log = LoggerFactory.getLogger(ClassificationService.class);

    public String classify(String sensorId, FftAnalysisService.FftResult fftResult) {
        double dominantFreq = fftResult.dominantFrequencyHz();
        double magnitude = fftResult.magnitude();

        double NOISE_THRESHOLD = 0.5;
        if (dominantFreq < NOISE_THRESHOLD) {
            log.debug("Sensor {} below noise threshold ({} Hz) — discarding window",
                    sensorId, dominantFreq);
            return null;
        }

        log.info("Sensor {} dominant frequency: {} Hz, magnitude: {}",
                sensorId, dominantFreq, magnitude);

        String eventType;
        if (dominantFreq < 3.0) {
            eventType = "earthquake";
        } else if (dominantFreq < 8.0) {
            eventType = "conventional_explosion";
        } else {
            eventType = "nuclear_like";
        }

        return eventType;
    }

    public record ClassificationResult(
            String eventType,
            double dominantFrequencyHz,
            double magnitude
    ) {}
}
old-private-repo/processing-classification
