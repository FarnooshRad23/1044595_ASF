package com.advprog.processing.service;


import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.advprog.processing.dto.HistoricalEventRow;

@Service
public class EventHistoryQueryService {

    private final JdbcTemplate jdbc;

    public EventHistoryQueryService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<HistoricalEventRow> search(
            String sensorIdLike,
            String region,
            String type,
            OffsetDateTime fromInclusive,
            OffsetDateTime toExclusive,
            int limit,
            int offset
    ) {
        int safeLimit = Math.min(Math.max(limit, 1), 1000);
        int safeOffset = Math.max(offset, 0);

        StringBuilder sql = new StringBuilder("""
            SELECT id, sensor_id, region, window_start, event_type,
                   dominant_freq_hz, magnitude
            FROM detected_events
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();

        if (sensorIdLike != null && !sensorIdLike.isBlank()) {
            // ILIKE = LIKE case-insensitive in PostgreSQL (estensione PG). citeturn2search2
            sql.append(" AND sensor_id ILIKE ?");
            params.add("%" + sensorIdLike + "%");
        }

        if (region != null && !region.isBlank()) {
            sql.append(" AND region = ?");
            params.add(region);
        }

        if (type != null && !type.isBlank()) {
            sql.append(" AND event_type = ?");
            params.add(type);
        }

        if (fromInclusive != null && toExclusive != null) {
            sql.append(" AND window_start >= ? AND window_start < ?");
            params.add(fromInclusive);
            params.add(toExclusive);
        }

        sql.append(" ORDER BY window_start DESC LIMIT ? OFFSET ?");
        params.add(safeLimit);
        params.add(safeOffset);

        return jdbc.query(sql.toString(), params.toArray(), (rs, rowNum) -> mapRow(rs));
    }

    private HistoricalEventRow mapRow(ResultSet rs) throws SQLException {
        String id = String.valueOf(rs.getLong("id"));
        String sensorId = rs.getString("sensor_id");
        String region = rs.getString("region");
        String type = rs.getString("event_type");

        // Se la colonna è timestamptz, leggerla come OffsetDateTime è robusto.
        OffsetDateTime windowStart = rs.getObject("window_start", OffsetDateTime.class);

        double freq = rs.getDouble("dominant_freq_hz");
        double amp = rs.getDouble("magnitude"); // mappo magnitude -> amp (coerente con UI)

        return new HistoricalEventRow(
                id,
                sensorId,
                region,
                windowStart != null ? windowStart.toString() : null,
                type,
                freq,
                amp
        );
    }
}