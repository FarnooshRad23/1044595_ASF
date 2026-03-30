package com.advprog.processing.service;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.springframework.stereotype.Service;

@Service
public class FftAnalysisService {

    private final FastFourierTransformer fft =
        new FastFourierTransformer(DftNormalization.STANDARD);

    /**
     * Runs FFT on the 128-sample window and returns the dominant frequency in Hz.
     * Bin 0 (DC component) is always skipped.
     *
     * @param samples        128 double values from the sliding window
     * @param samplingRateHz sensor's sampling rate (from SensorSummary)
     * @return dominant frequency in Hz
     */
    public double dominantFrequencyHz(double[] samples, double samplingRateHz) {
        Complex[] spectrum = fft.transform(samples, TransformType.FORWARD);

        int dominantBin = 1; // skip bin 0 (DC)
        double maxMagnitude = 0.0;
        int halfLen = spectrum.length / 2; // only positive frequencies

        for (int k = 1; k < halfLen; k++) {
            double mag = spectrum[k].abs();
            if (mag > maxMagnitude) {
                maxMagnitude = mag;
                dominantBin = k;
            }
        }

        return (double) dominantBin * samplingRateHz / samples.length;
    }
}
