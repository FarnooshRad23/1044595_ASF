package com.advprog.processing.service;

HEAD

import org.apache.commons.math3.complex.Complex;
>>>>>>> old-private-repo/processing-classification
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.springframework.stereotype.Service;
HEAD

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
old-private-repo/processing-classification
