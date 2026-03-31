package com.advprog.processing.service;

import com.advprog.processing.dto.SensorSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SensorCacheServiceTest {

    private static final String SIMULATOR_URL = "http://simulator";

    /** 12-sensor JSON payload — mirrors what the real simulator returns. */
    private static final String TWELVE_SENSORS_JSON = """
            [
              {"id":"sensor-01","name":"Alpha","category":"seismic","region":"North","coordinates":{"latitude":41.9,"longitude":12.5},"measurement_unit":"m/s²","sampling_rate_hz":100.0,"websocket_url":"/ws/sensor-01"},
              {"id":"sensor-02","name":"Beta","category":"seismic","region":"North","coordinates":{"latitude":42.0,"longitude":12.6},"measurement_unit":"m/s²","sampling_rate_hz":100.0,"websocket_url":"/ws/sensor-02"},
              {"id":"sensor-03","name":"Gamma","category":"seismic","region":"South","coordinates":{"latitude":40.5,"longitude":14.2},"measurement_unit":"m/s²","sampling_rate_hz":100.0,"websocket_url":"/ws/sensor-03"},
              {"id":"sensor-04","name":"Delta","category":"seismic","region":"South","coordinates":{"latitude":40.6,"longitude":14.3},"measurement_unit":"m/s²","sampling_rate_hz":100.0,"websocket_url":"/ws/sensor-04"},
              {"id":"sensor-05","name":"Epsilon","category":"seismic","region":"East","coordinates":{"latitude":43.1,"longitude":13.7},"measurement_unit":"m/s²","sampling_rate_hz":50.0,"websocket_url":"/ws/sensor-05"},
              {"id":"sensor-06","name":"Zeta","category":"seismic","region":"East","coordinates":{"latitude":43.2,"longitude":13.8},"measurement_unit":"m/s²","sampling_rate_hz":50.0,"websocket_url":"/ws/sensor-06"},
              {"id":"sensor-07","name":"Eta","category":"seismic","region":"West","coordinates":{"latitude":41.0,"longitude":11.0},"measurement_unit":"m/s²","sampling_rate_hz":50.0,"websocket_url":"/ws/sensor-07"},
              {"id":"sensor-08","name":"Theta","category":"seismic","region":"West","coordinates":{"latitude":41.1,"longitude":11.1},"measurement_unit":"m/s²","sampling_rate_hz":50.0,"websocket_url":"/ws/sensor-08"},
              {"id":"sensor-09","name":"Iota","category":"seismic","region":"Central","coordinates":{"latitude":41.8,"longitude":12.3},"measurement_unit":"m/s²","sampling_rate_hz":200.0,"websocket_url":"/ws/sensor-09"},
              {"id":"sensor-10","name":"Kappa","category":"seismic","region":"Central","coordinates":{"latitude":41.9,"longitude":12.4},"measurement_unit":"m/s²","sampling_rate_hz":200.0,"websocket_url":"/ws/sensor-10"},
              {"id":"sensor-11","name":"Lambda","category":"seismic","region":"North","coordinates":{"latitude":42.1,"longitude":12.7},"measurement_unit":"m/s²","sampling_rate_hz":100.0,"websocket_url":"/ws/sensor-11"},
              {"id":"sensor-12","name":"Mu","category":"seismic","region":"South","coordinates":{"latitude":40.4,"longitude":14.1},"measurement_unit":"m/s²","sampling_rate_hz":100.0,"websocket_url":"/ws/sensor-12"}
            ]
            """;

    private RestClient.Builder builder;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        // Must bind BEFORE constructing SensorCacheService (which calls builder.build() internally)
        mockServer = MockRestServiceServer.bindTo(builder).build();
    }

    @Test
    void loadCache_populatesAllTwelveEntries() {
        mockServer.expect(requestTo(SIMULATOR_URL + "/api/devices/"))
                .andRespond(withSuccess(TWELVE_SENSORS_JSON, MediaType.APPLICATION_JSON));

        SensorCacheService service = new SensorCacheService(builder, SIMULATOR_URL);
        service.loadCache();

        assertThat(service.getAll()).hasSize(12);

        // Spot-check snake_case → camelCase field mapping
        SensorSummary sensor05 = service.getAll().get("sensor-05");
        assertThat(sensor05).isNotNull();
        assertThat(sensor05.name()).isEqualTo("Epsilon");
        assertThat(sensor05.samplingRateHz()).isEqualTo(50.0);
        assertThat(sensor05.websocketUrl()).isEqualTo("/ws/sensor-05");
        assertThat(sensor05.measurementUnit()).isEqualTo("m/s²");
        assertThat(sensor05.coordinates().latitude()).isEqualTo(43.1);
        assertThat(sensor05.coordinates().longitude()).isEqualTo(13.7);

        mockServer.verify();
    }

    @Test
    void get_presentEntry_returnsCachedValueWithoutRefetch() {
        // Only one HTTP call expected total
        mockServer.expect(requestTo(SIMULATOR_URL + "/api/devices/"))
                .andRespond(withSuccess(TWELVE_SENSORS_JSON, MediaType.APPLICATION_JSON));

        SensorCacheService service = new SensorCacheService(builder, SIMULATOR_URL);
        service.loadCache();

        Optional<SensorSummary> result = service.get("sensor-05");

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("Epsilon");
        mockServer.verify(); // exactly 1 call — no extra fetch triggered
    }

    @Test
    void get_missedEntry_triggersLazyRefetchAndReturnsValue() {
        // First call: loadCache(); second call: triggered by cache miss in get()
        mockServer.expect(requestTo(SIMULATOR_URL + "/api/devices/"))
                .andRespond(withSuccess(TWELVE_SENSORS_JSON, MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(SIMULATOR_URL + "/api/devices/"))
                .andRespond(withSuccess(TWELVE_SENSORS_JSON, MediaType.APPLICATION_JSON));

        SensorCacheService service = new SensorCacheService(builder, SIMULATOR_URL);
        service.loadCache();

        // Simulate a cache miss by removing the entry after initial load
        service.getAll().remove("sensor-05");

        Optional<SensorSummary> result = service.get("sensor-05");

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("Epsilon");
        mockServer.verify(); // exactly 2 HTTP calls
    }

    @Test
    void get_unknownEntry_returnsEmptyAfterRefetch() {
        // First call: loadCache(); second call: triggered by cache miss for unknown id
        mockServer.expect(requestTo(SIMULATOR_URL + "/api/devices/"))
                .andRespond(withSuccess(TWELVE_SENSORS_JSON, MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(SIMULATOR_URL + "/api/devices/"))
                .andRespond(withSuccess(TWELVE_SENSORS_JSON, MediaType.APPLICATION_JSON));

        SensorCacheService service = new SensorCacheService(builder, SIMULATOR_URL);
        service.loadCache();

        Optional<SensorSummary> result = service.get("sensor-99");

        assertThat(result).isEmpty();
        mockServer.verify(); // exactly 2 HTTP calls
    }
}
