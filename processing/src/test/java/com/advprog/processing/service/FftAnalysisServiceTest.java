package com.advprog.processing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class FftAnalysisServiceTest {

    @Test
    void sineWaveAt2Hz_returnsDominantFreqNear2Hz() {
        // samplingRateHz=64 makes 2 Hz land exactly on bin 4 (2×128/64=4) — no spectral leakage
        double samplingRateHz = 64.0;
        double[] samples = new double[128];
        for (int i = 0; i < 128; i++) {
            samples[i] = Math.sin(2 * Math.PI * 2.0 * i / samplingRateHz);
        }

        FftAnalysisService service = new FftAnalysisService();
        double result = service.dominantFrequencyHz(samples, samplingRateHz);

        assertThat(result).isCloseTo(2.0, within(0.1));
    }
}
