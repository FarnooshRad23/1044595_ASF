package com.advprog.processing.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ControlStreamServiceTest {

    private static final String SIMULATOR_URL = "http://simulator";

    private RestClient.Builder builder;
    private MockRestServiceServer mockServer;
    private AtomicBoolean exitCalled;
    private AtomicInteger exitCode;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        // Must bind BEFORE constructing ControlStreamService (which calls builder.build() internally)
        mockServer = MockRestServiceServer.bindTo(builder).build();
        exitCalled = new AtomicBoolean(false);
        exitCode = new AtomicInteger(-1);
    }

    private ControlStreamService buildService() {
        return new ControlStreamService(builder, SIMULATOR_URL, code -> {
            exitCalled.set(true);
            exitCode.set(code);
        });
    }

    @Test
    void shutdownCommand_invokesExitHandler() throws Exception {
        // Verifies the core requirement: a SHUTDOWN command causes the replica to exit with code 1.
        String sse = "event: command\ndata: {\"command\":\"SHUTDOWN\"}\n\n";
        mockServer.expect(requestTo(SIMULATOR_URL + "/api/control"))
                .andRespond(withSuccess(sse, MediaType.TEXT_EVENT_STREAM));

        buildService().listenOnce();

        assertThat(exitCalled.get()).isTrue();
        assertThat(exitCode.get()).isEqualTo(1);
        mockServer.verify();
    }

    @Test
    void heartbeatEvent_doesNotTriggerExit() throws Exception {
        // Heartbeat events must be silently ignored; only "command" events act.
        String sse = "event: heartbeat\ndata: {\"timestamp\":\"2026-01-01T00:00:00Z\",\"controlStreamConnections\":1}\n\n";
        mockServer.expect(requestTo(SIMULATOR_URL + "/api/control"))
                .andRespond(withSuccess(sse, MediaType.TEXT_EVENT_STREAM));

        buildService().listenOnce();

        assertThat(exitCalled.get()).isFalse();
        mockServer.verify();
    }

    @Test
    void controlOpenEvent_doesNotTriggerExit() throws Exception {
        // Connection-open events carry metadata but must not trigger shutdown.
        String sse = "event: control-open\ndata: {\"connectedAt\":\"2026-01-01T00:00:00Z\",\"controlStreamConnections\":1}\n\n";
        mockServer.expect(requestTo(SIMULATOR_URL + "/api/control"))
                .andRespond(withSuccess(sse, MediaType.TEXT_EVENT_STREAM));

        buildService().listenOnce();

        assertThat(exitCalled.get()).isFalse();
        mockServer.verify();
    }

    @Test
    void multipleEvents_onlyShutdownTriggers() throws Exception {
        // Mixed stream: heartbeat first, then SHUTDOWN — exit must fire exactly once.
        String sse = "event: heartbeat\ndata: {\"timestamp\":\"2026-01-01T00:00:00Z\",\"controlStreamConnections\":1}\n\n"
                   + "event: command\ndata: {\"command\":\"SHUTDOWN\"}\n\n";
        mockServer.expect(requestTo(SIMULATOR_URL + "/api/control"))
                .andRespond(withSuccess(sse, MediaType.TEXT_EVENT_STREAM));

        buildService().listenOnce();

        assertThat(exitCalled.get()).isTrue();
        assertThat(exitCode.get()).isEqualTo(1);
        mockServer.verify();
    }
}
