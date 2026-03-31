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
