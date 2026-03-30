package com.advprog.broker.service;

import com.advprog.broker.dto.Coordinates;
import com.advprog.broker.dto.SensorSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SensorDiscoveryServiceTest {

    private static final String BASE_URL = "http://localhost:8080";
    private static final String DEVICES_URL = BASE_URL + "/api/devices/";

    @Mock
    private RestTemplate restTemplate;

    private SensorDiscoveryService service;

    @BeforeEach
    void setUp() {
        service = new SensorDiscoveryService(restTemplate);
        ReflectionTestUtils.setField(service, "simulatorBaseUrl", BASE_URL);
    }

    // ── happy path ────────────────────────────────────────────────────────────

    @Test
    void discoverSensors_populatesSensorList() {
        SensorSummary[] payload = buildSensors(
            new SensorSummary("sensor-01", "Alpha", "seismic", "Europe",
                new Coordinates(41.9, 12.5), "m/s²", 100.0, "/ws/sensor-01"),
            new SensorSummary("sensor-02", "Beta",  "seismic", "Asia",
                new Coordinates(35.7, 139.7), "m/s²", 100.0, "/ws/sensor-02")
        );
        when(restTemplate.getForObject(DEVICES_URL, SensorSummary[].class)).thenReturn(payload);

        service.discoverSensors();

        assertThat(service.getSensors()).hasSize(2);
    }

    @Test
    void discoverSensors_mapsAllFieldsCorrectly() {
        SensorSummary expected = new SensorSummary(
            "sensor-03", "Gamma", "nuclear", "Americas",
            new Coordinates(40.7, -74.0), "m/s²", 200.0, "/ws/sensor-03"
        );
        when(restTemplate.getForObject(DEVICES_URL, SensorSummary[].class))
            .thenReturn(new SensorSummary[]{expected});

        service.discoverSensors();

        SensorSummary actual = service.getSensors().get(0);
        assertThat(actual.id()).isEqualTo("sensor-03");
        assertThat(actual.name()).isEqualTo("Gamma");
        assertThat(actual.category()).isEqualTo("nuclear");
        assertThat(actual.region()).isEqualTo("Americas");
        assertThat(actual.coordinates().latitude()).isEqualTo(40.7);
        assertThat(actual.coordinates().longitude()).isEqualTo(-74.0);
        assertThat(actual.measurement_unit()).isEqualTo("m/s²");
        assertThat(actual.sampling_rate_hz()).isEqualTo(200.0);
        assertThat(actual.websocket_url()).isEqualTo("/ws/sensor-03");
    }

    @Test
    void discoverSensors_allTwelveSensorsAreStored() {
        SensorSummary[] twelve = new SensorSummary[12];
        for (int i = 1; i <= 12; i++) {
            String id = String.format("sensor-%02d", i);
            twelve[i - 1] = new SensorSummary(id, "Sensor " + i, "seismic", "Region",
                new Coordinates(0, 0), "m/s²", 100.0, "/ws/" + id);
        }
        when(restTemplate.getForObject(DEVICES_URL, SensorSummary[].class)).thenReturn(twelve);

        service.discoverSensors();

        List<SensorSummary> sensors = service.getSensors();
        assertThat(sensors).hasSize(12);
        assertThat(sensors).extracting(SensorSummary::id)
            .containsExactly(
                "sensor-01", "sensor-02", "sensor-03", "sensor-04",
                "sensor-05", "sensor-06", "sensor-07", "sensor-08",
                "sensor-09", "sensor-10", "sensor-11", "sensor-12"
            );
    }

    // ── edge cases ────────────────────────────────────────────────────────────

    @Test
    void discoverSensors_emptyArray_resultIsEmptyList() {
        when(restTemplate.getForObject(DEVICES_URL, SensorSummary[].class))
            .thenReturn(new SensorSummary[0]);

        service.discoverSensors();

        assertThat(service.getSensors()).isEmpty();
    }

    @Test
    void discoverSensors_nullResponse_resultIsEmptyList() {
        when(restTemplate.getForObject(DEVICES_URL, SensorSummary[].class)).thenReturn(null);

        service.discoverSensors();

        assertThat(service.getSensors()).isEmpty();
    }

    // ── fault tolerance ───────────────────────────────────────────────────────

    @Test
    void discoverSensors_simulatorUnreachable_doesNotCrash() {
        when(restTemplate.getForObject(DEVICES_URL, SensorSummary[].class))
            .thenThrow(new ResourceAccessException("Connection refused"));

        // must not throw
        service.discoverSensors();

        assertThat(service.getSensors()).isEmpty();
    }

    @Test
    void discoverSensors_unexpectedException_doesNotCrash() {
        when(restTemplate.getForObject(DEVICES_URL, SensorSummary[].class))
            .thenThrow(new RuntimeException("unexpected"));

        service.discoverSensors();

        assertThat(service.getSensors()).isEmpty();
    }

    @Test
    void getSensors_beforeDiscovery_returnsEmptyList() {
        // no discoverSensors() called — default state
        assertThat(service.getSensors()).isEmpty();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private SensorSummary[] buildSensors(SensorSummary... sensors) {
        return sensors;
    }
}
