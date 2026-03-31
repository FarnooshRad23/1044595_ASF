package com.advprog.processing.service;

import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.springframework.stereotype.Service;

@Service
public class FftAnalysisService {

    public record FftResult(double dominantFreqHz, double magnitude) {}

    private static final FastFourierTransformer TRANSFORMER =
            new FastFourierTransformer(DftNormalization.STANDARD);

    public FftResult analyze(double[] samples, double samplingRateHz) {
        var complex = TRANSFORMER.transform(samples, TransformType.FORWARD);

        int bestBin = 1;
        double bestMag = 0.0;
        int nyquist = complex.length / 2;

        for (int i = 1; i <= nyquist; i++) {
            double re = complex[i].getReal();
            double im = complex[i].getImaginary();
            double mag = Math.sqrt(re * re + im * im);
            if (mag > bestMag) {
                bestMag = mag;
                bestBin = i;
            }
        }

        double dominantFreqHz = (double) bestBin * samplingRateHz / samples.length;
        return new FftResult(dominantFreqHz, bestMag);
    }
}
