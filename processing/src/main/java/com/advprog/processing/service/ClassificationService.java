package com.advprog.processing.service;

 

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.advprog.processing.service.SlidingWindowService.WindowResult;
@Service

public class ClassificationService {
 private static final Logger log = LoggerFactory.getLogger(ClassificationService.class);

    public void analyze(WindowResult result) {
       List<Double> values = Arrays.stream(result.samples())
        .boxed()
        .toList();
        List<ComplexNumber> spectrum = dft(values);

        log.info("DFT for sensor {}:", result.sensorId());
        for (int k = 0; k < spectrum.size(); k++) {
            ComplexNumber c = spectrum.get(k);
            String type="no event";
            if (c.magnitude()>=0.5 && c.magnitude()<3.0){
               type="earthquake";
            }
            if (c.magnitude()>=3.0 && c.magnitude()<8){
               type="conventional explasion";
            }
            if (c.magnitude()>=8){
               type="Nuclear-like event";
            }
            log.info(" magnitude={}, event type:{}",
                c.magnitude(),type);
        }
    }

    private List<ComplexNumber> dft(List<Double> values) {
        int n = values.size();
        java.util.List<ComplexNumber> output = new java.util.ArrayList<>();

        for (int k = 0; k < n; k++) {
            double real = 0.0;
            double imag = 0.0;

            for (int t = 0; t < n; t++) {
                double angle = -2.0 * Math.PI * k * t / n;
                real += values.get(t) * Math.cos(angle);
                imag += values.get(t) * Math.sin(angle);
            }

            output.add(new ComplexNumber(real, imag));
        }

        return output;
    }

    public record ComplexNumber(double real, double imag) {
        public double magnitude() {
            return Math.sqrt(real * real + imag * imag);
        }
    }
}