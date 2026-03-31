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

    public FftResult analyze(double[] samples, double samplingRateHz) {
        Complex[] spectrum = fft.transform(samples, TransformType.FORWARD);

        int dominantBin = 1;
        double maxMagnitude = 0.0;
        int halfLen = spectrum.length / 2;

        for (int k = 1; k < halfLen; k++) {
            double mag = spectrum[k].abs();
            if (mag > maxMagnitude) {
                maxMagnitude = mag;
                dominantBin = k;
            }
        }

        double dominantFreq = (double) dominantBin * samplingRateHz / samples.length;

        return new FftResult(dominantFreq, maxMagnitude);
    }

    
    public record FftResult(double dominantFrequencyHz, double magnitude) {}
}