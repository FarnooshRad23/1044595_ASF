package com.advprog.processing.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class FftAnalysisServiceTest {

    private final FftAnalysisService fft = new FftAnalysisService();

    private static final double SAMPLING_RATE = 20.0;
    private static final int WINDOW = 128;

    @Test
    void sineWave2Hz_detectsApprox2Hz() {
        double[] samples = new double[WINDOW];
        for (int i = 0; i < WINDOW; i++) {
            samples[i] = Math.sin(2 * Math.PI * 2.0 * i / SAMPLING_RATE);
        }

        FftAnalysisService.FftResult result = fft.analyze(samples, SAMPLING_RATE);

        assertThat(result.dominantFreqHz()).isCloseTo(2.0, within(0.2));
        assertThat(result.magnitude()).isPositive();
    }

    @Test
    void flatSignal_dcBinSkipped_noDominantAtZeroHz() {
        double[] samples = new double[WINDOW];
        for (int i = 0; i < WINDOW; i++) {
            samples[i] = 5.0; // constant — all energy in bin 0 (DC)
        }

        FftAnalysisService.FftResult result = fft.analyze(samples, SAMPLING_RATE);

        // DC bin is skipped, so dominant freq must be > 0
        assertThat(result.dominantFreqHz()).isGreaterThan(0.0);
    }
}
