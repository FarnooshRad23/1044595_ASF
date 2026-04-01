package com.advprog.processing.controller;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.advprog.processing.dto.HistoricalEventRow;
import com.advprog.processing.service.EventHistoryQueryService;

@RestController
public class EventsController {

    private final EventHistoryQueryService history;

    public EventsController(EventHistoryQueryService history) {
        this.history = history;
    }

    @GetMapping("/api/events")
    public List<HistoricalEventRow> getEvents(
            @RequestParam(name = "sensorId", required = false) String sensorIdLike,
            @RequestParam(name = "region", required = false) String region,
            @RequestParam(name = "type", required = false) String type,

            // Filtro per giorno (YYYY-MM-DD)
            @RequestParam(name = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,

            @RequestParam(name = "limit", defaultValue = "200") int limit,
            @RequestParam(name = "offset", defaultValue = "0") int offset
    ) {
        OffsetDateTime from = null;
        OffsetDateTime to = null;

        // Assunzione: interpretiamo il filtro data come giorno UTC.
        if (date != null) {
            from = date.atStartOfDay().atOffset(ZoneOffset.UTC);
            to = date.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        }

        return history.search(sensorIdLike, region, type, from, to, limit, offset);
    }
}