package com.advprog.processing.dto;

<<<<<<< HEAD
public record Coordinates(double latitude, double longitude) {}
=======
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.List;

public record Coordinates(double latitude, double longitude) {

    @JsonCreator
    public static Coordinates fromArray(List<Double> coords) {
        return new Coordinates(coords.get(0), coords.get(1));
    }
}
>>>>>>> old-private-repo/frontendtobackend
