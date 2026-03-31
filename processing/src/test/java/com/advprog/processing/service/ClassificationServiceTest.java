package com.advprog.processing.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClassificationServiceTest {

    private final ClassificationService classifier = new ClassificationService();

    @Test
    void noise_returnsEmpty() {
        assertThat(classifier.classify(0.3)).isEmpty();
    }

    @Test
    void earthquake_midRange() {
        assertThat(classifier.classify(1.5)).hasValue("earthquake");
    }

    @Test
    void conventionalExplosion_midRange() {
        assertThat(classifier.classify(5.0)).hasValue("conventional_explosion");
    }

    @Test
    void nuclearLike_midRange() {
        assertThat(classifier.classify(9.0)).hasValue("nuclear_like");
    }

    @Test
    void boundary_0_5Hz_isEarthquake() {
        assertThat(classifier.classify(0.5)).hasValue("earthquake");
    }

    @Test
    void boundary_3_0Hz_isConventionalExplosion() {
        assertThat(classifier.classify(3.0)).hasValue("conventional_explosion");
    }

    @Test
    void boundary_8_0Hz_isNuclearLike() {
        assertThat(classifier.classify(8.0)).hasValue("nuclear_like");
    }
}
