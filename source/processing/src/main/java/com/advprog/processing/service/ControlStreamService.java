package com.advprog.processing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Consumer;

/**
 * Connects to the simulator's SSE control stream and shuts down this replica
 * when a SHUTDOWN command is received. Non-zero exit (1) ensures Docker's
 * restart: on-failure policy triggers a clean restart.
 */
@Service
public class ControlStreamService {

    private static final Logger log = LoggerFactory.getLogger(ControlStreamService.class);

    private final RestClient restClient;
    private final Consumer<Integer> exitHandler;
    private final ObjectMapper objectMapper = new ObjectMapper();
<<<<<<< HEAD
    private final String simulatorUrl;
=======
>>>>>>> old-private-repo/frontendtobackend

    private volatile boolean controlConnected = false;

    /** Production constructor — exits the JVM via System.exit. */
    @Autowired
    public ControlStreamService(RestClient.Builder builder,
                                @Value("${simulator.url}") String simulatorUrl) {
        this(builder, simulatorUrl, System::exit);
    }

    /** Package-private constructor for tests — accepts an injectable exit handler. */
    ControlStreamService(RestClient.Builder builder,
                         String simulatorUrl,
                         Consumer<Integer> exitHandler) {
        this.restClient = builder.baseUrl(simulatorUrl).build();
<<<<<<< HEAD
        this.simulatorUrl = simulatorUrl;
=======
>>>>>>> old-private-repo/frontendtobackend
        this.exitHandler = exitHandler;
    }

    /**
     * Starts a daemon thread that calls listenOnce() in a retry loop.
     * Daemon thread: won't block JVM shutdown initiated by other means.
     */
    @PostConstruct
    public void init() {
        Thread thread = new Thread(() -> {
            while (true) {
                try {
                    listenOnce();
                } catch (Exception e) {
                    log.warn("ControlStreamService: SSE connection lost ({}). Retrying in 5s.", e.getMessage());
                    try {
                        Thread.sleep(5_000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }, "control-stream-listener");
        // Daemon thread: won't prevent JVM shutdown if Spring context exits first.
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Opens the SSE stream and blocks until EOF or an exception.
     * Tests call this directly to avoid thread timing issues.
     */
    void listenOnce() throws Exception {
        controlConnected = true;
<<<<<<< HEAD
        log.info("ControlStreamService: connecting to SSE at {}/api/control", simulatorUrl);
=======
>>>>>>> old-private-repo/frontendtobackend
        try {
            // exchange() gives direct InputStream access needed for line-by-line SSE parsing.
            // Checked exceptions from parseSseStream are wrapped and re-thrown after the call.
            Exception[] caught = {null};
            restClient.get()
                    .uri("/api/control")
                    .exchange((request, response) -> {
                        try (InputStream body = response.getBody();
                             BufferedReader reader = new BufferedReader(new InputStreamReader(body))) {
                            parseSseStream(reader);
                        } catch (Exception e) {
                            caught[0] = e;
                        }
                        return null;
                    });
            if (caught[0] != null) throw caught[0];
        } finally {
            controlConnected = false;
        }
    }

    public boolean isControlConnected() { return controlConnected; }

    private void parseSseStream(BufferedReader reader) throws Exception {
        String currentEvent = null;
        String line;

        while ((line = reader.readLine()) != null) {
            if (line.startsWith("event:")) {
                // Record the event type for the next data line.
                currentEvent = line.substring("event:".length()).trim();
            } else if (line.startsWith("data:")) {
<<<<<<< HEAD
                String json = line.substring("data:".length()).trim();
                if ("control-open".equals(currentEvent)) {
                    log.info("ControlStreamService: SSE stream connected — {}", json);
                } else if ("heartbeat".equals(currentEvent)) {
                    log.debug("ControlStreamService: heartbeat — {}", json);
                } else if ("command".equals(currentEvent)) {
=======
                if ("command".equals(currentEvent)) {
                    String json = line.substring("data:".length()).trim();
>>>>>>> old-private-repo/frontendtobackend
                    JsonNode node = objectMapper.readTree(json);
                    JsonNode commandNode = node.get("command");
                    if (commandNode != null && "SHUTDOWN".equals(commandNode.asText())) {
                        log.info("ControlStreamService: SHUTDOWN received — exiting with code 1");
                        // exit(1) triggers Docker restart: on-failure; exit(0) would not.
                        exitHandler.accept(1);
                    }
                }
            } else if (line.isEmpty()) {
                // Empty line is the SSE event separator; reset the event type.
                currentEvent = null;
            }
        }
    }
}
