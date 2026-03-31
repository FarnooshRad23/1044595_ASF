package com.advprog.processing.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for EventPersistenceService.
 *
 * The ON CONFLICT DO NOTHING deduplication is a database feature; here we verify
 * that persist() correctly interprets the JDBC update count (1 = inserted, 0 = deduped)
 * and returns the right boolean.
 */
class EventPersistenceServiceTest {

    private JdbcTemplate jdbc;
    private EventPersistenceService service;

    private static final String SENSOR_ID    = "sensor-01";
    private static final String EVENT_TYPE   = "earthquake";
    private static final String WINDOW_START = "2024-01-01T00:00:00Z";
    private static final String WINDOW_END   = "2024-01-01T00:00:06Z";

    @BeforeEach
    void setUp() throws Exception {
        jdbc = mock(JdbcTemplate.class);
        service = new EventPersistenceService(jdbc);

        Field f = EventPersistenceService.class.getDeclaredField("replicaId");
        f.setAccessible(true);
        f.set(service, "test-replica");
    }

    @Test
    void persist_jdbcReturnsOne_returnsTrue() {
        when(jdbc.update(any(String.class), any(Object[].class))).thenReturn(1);

        boolean result = service.persist(SENSOR_ID, "Alpha", EVENT_TYPE,
                1.5, 0.8, WINDOW_START, WINDOW_END, "North", "seismic");

        assertThat(result).isTrue();
    }

    @Test
    void persist_jdbcReturnsZero_returnsFalse() {
        // Simulates ON CONFLICT DO NOTHING deduplication (DB skipped the insert)
        when(jdbc.update(any(String.class), any(Object[].class))).thenReturn(0);

        boolean result = service.persist(SENSOR_ID, "Alpha", EVENT_TYPE,
                1.5, 0.8, WINDOW_START, WINDOW_END, "North", "seismic");

        assertThat(result).isFalse();
    }

    @Test
    void persist_parsesTimestampsWithoutThrowing() {
        when(jdbc.update(any(String.class), any(Object[].class))).thenReturn(1);

        // ISO-8601 strings from the sliding window — must parse cleanly to OffsetDateTime
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() ->
                service.persist(SENSOR_ID, "Alpha", EVENT_TYPE,
                        1.5, 0.8,
                        "2024-06-15T12:34:56.789Z",
                        "2024-06-15T12:35:02.789Z",
                        "North", "seismic"));
    }
}
