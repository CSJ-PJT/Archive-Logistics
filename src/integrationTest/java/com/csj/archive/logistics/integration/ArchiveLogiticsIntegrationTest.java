package com.csj.archive.logistics.integration;

import com.csj.archive.logistics.audit.AuditAction;
import com.csj.archive.logistics.audit.AuditLogRepository;
import com.csj.archive.logistics.event.NexusEventRepository;
import com.csj.archive.logistics.event.NexusEventStatus;
import com.csj.archive.logistics.outbox.LedgerPublishAttemptRepository;
import com.csj.archive.logistics.outbox.LogisticsOutboxRepository;
import com.csj.archive.logistics.outbox.OutboxStatus;
import com.csj.archive.logistics.route.RouteCostRepository;
import com.csj.archive.logistics.route.RoutePlanRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ArchiveLogiticsIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("archive_logitics_test")
            .withUsername("archive")
            .withPassword("archive");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("archive.ledger.enabled", () -> "false");
        registry.add("archive.outbox.scheduler.enabled", () -> "false");
        registry.add("spring.task.scheduling.enabled", () -> "false");
    }

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    NexusEventRepository nexusEventRepository;

    @Autowired
    RoutePlanRepository routePlanRepository;

    @Autowired
    RouteCostRepository routeCostRepository;

    @Autowired
    LogisticsOutboxRepository outboxRepository;

    @Autowired
    LedgerPublishAttemptRepository attemptRepository;

    @Autowired
    AuditLogRepository auditLogRepository;

    @BeforeEach
    void cleanDatabase() {
        auditLogRepository.deleteAllInBatch();
        attemptRepository.deleteAllInBatch();
        outboxRepository.deleteAllInBatch();
        routeCostRepository.deleteAllInBatch();
        routePlanRepository.deleteAllInBatch();
        nexusEventRepository.deleteAllInBatch();
    }

    @Test
    void nexusEventCreatesRouteCostAndOutbox() {
        ResponseEntity<JsonNode> response = post("/api/events/nexus",
                event("single-001", "FAC-A", "DC-SEOUL-01", "HIGH", false));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = response.getBody().path("data");
        assertThat(data.path("status").asText()).isEqualTo("PROCESSED");
        assertThat(data.path("duplicate").asBoolean()).isFalse();
        assertThat(data.path("routePlanId").asText()).startsWith("ROUTE-");
        assertThat(data.path("outboxEventId").asText()).startsWith("evt-logitics-");
        assertThat(data.path("totalCost").asLong()).isEqualTo(93_420L);

        assertThat(nexusEventRepository.countByStatus(NexusEventStatus.PROCESSED)).isEqualTo(1);
        assertThat(routePlanRepository.count()).isEqualTo(1);
        assertThat(routeCostRepository.count()).isEqualTo(1);
        assertThat(outboxRepository.countByStatus(OutboxStatus.PENDING)).isEqualTo(1);

        var outbox = outboxRepository.findAll().getFirst();
        assertThat(outbox.source()).isEqualTo("Archive-Logitics");
        assertThat(outbox.eventType()).isEqualTo("URGENT_DELIVERY_COST_CONFIRMED");
        assertThat(outbox.idempotencyKey()).startsWith("LOGISTICS:URGENT_DELIVERY_COST_CONFIRMED:ROUTE-");
        assertThat(auditLogRepository.countByAction(AuditAction.OUTBOX_EVENT_CREATED)).isEqualTo(1);
    }

    @Test
    void duplicateEventIdAndIdempotencyKeyDoNotCreateDuplicateRouteOrOutbox() {
        Map<String, Object> original = event("dup-001", "FAC-B", "DC-DAEJEON-01", "NORMAL", false);
        assertThat(post("/api/events/nexus", original).getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<JsonNode> duplicateEventId = post("/api/events/nexus", original);
        assertThat(duplicateEventId.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(duplicateEventId.getBody().path("data").path("duplicate").asBoolean()).isTrue();

        Map<String, Object> sameIdempotency = new HashMap<>(
                event("dup-002", "FAC-B", "DC-DAEJEON-01", "NORMAL", false)
        );
        sameIdempotency.put("idempotencyKey", original.get("idempotencyKey"));
        ResponseEntity<JsonNode> duplicateIdempotency = post("/api/events/nexus", sameIdempotency);
        assertThat(duplicateIdempotency.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(duplicateIdempotency.getBody().path("data").path("duplicate").asBoolean()).isTrue();

        assertThat(nexusEventRepository.count()).isEqualTo(1);
        assertThat(routePlanRepository.count()).isEqualTo(1);
        assertThat(outboxRepository.count()).isEqualTo(1);
        assertThat(auditLogRepository.countByAction(AuditAction.DUPLICATE_EVENT_RECEIVED)).isEqualTo(2);
    }

    @Test
    void unknownRouteIsPersistedAsFailedAndAudited() {
        ResponseEntity<JsonNode> response = post("/api/events/nexus",
                event("unknown-001", "FAC-X", "DC-SEOUL-01", "NORMAL", false));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().path("code").asText()).isEqualTo("PROCESSING_FAILED");
        assertThat(nexusEventRepository.countByStatus(NexusEventStatus.FAILED)).isEqualTo(1);
        assertThat(routePlanRepository.count()).isZero();
        assertThat(outboxRepository.count()).isZero();
        assertThat(auditLogRepository.countByAction(AuditAction.NEXUS_EVENT_FAILED)).isEqualTo(1);
    }

    @Test
    void bulkProcessesOneThousandEventsInChunks() {
        List<Map<String, Object>> events = new ArrayList<>();
        for (int index = 0; index < 1_000; index++) {
            String origin = switch (index % 3) {
                case 0 -> "FAC-A";
                case 1 -> "FAC-B";
                default -> "FAC-C";
            };
            String destination = switch (index % 3) {
                case 0 -> "DC-SEOUL-01";
                case 1 -> "DC-DAEJEON-01";
                default -> "DC-BUSAN-01";
            };
            String priority = switch (index % 4) {
                case 0 -> "NORMAL";
                case 1 -> "HIGH";
                case 2 -> "CRITICAL";
                default -> "NORMAL";
            };
            events.add(event("bulk-" + index, origin, destination, priority, index % 11 == 0));
        }

        ResponseEntity<JsonNode> response = post("/api/events/nexus/bulk", Map.of("events", events));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = response.getBody().path("data");
        assertThat(data.path("requestedCount").asInt()).isEqualTo(1_000);
        assertThat(data.path("successCount").asInt()).isEqualTo(1_000);
        assertThat(data.path("duplicateCount").asInt()).isZero();
        assertThat(data.path("failedCount").asInt()).isZero();
        assertThat(data.path("outboxCreatedCount").asInt()).isEqualTo(1_000);
        assertThat(outboxRepository.countByStatus(OutboxStatus.PENDING)).isEqualTo(1_000);
        assertThat(routePlanRepository.count()).isEqualTo(1_000);
    }

    @Test
    void ledgerDisabledPublishIsDryRunAndDoesNotChangeOutboxStatus() {
        assertThat(post("/api/events/nexus", event("dryrun-001", "FAC-A", "DC-BUSAN-01", "HIGH", false))
                .getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<JsonNode> publish = post("/api/outbox/publish", null);

        assertThat(publish.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = publish.getBody().path("data");
        assertThat(data.path("dryRun").asBoolean()).isTrue();
        assertThat(data.path("status").asText()).isEqualTo("DRY_RUN");
        assertThat(data.path("requestedCount").asInt()).isEqualTo(1);
        assertThat(outboxRepository.countByStatus(OutboxStatus.PENDING)).isEqualTo(1);
        assertThat(attemptRepository.count()).isEqualTo(1);
        assertThat(auditLogRepository.countByAction(AuditAction.OUTBOX_PUBLISH_SKIPPED)).isEqualTo(1);
    }

    @Test
    void simulationAndOperationsSummaryReturnCounts() {
        ResponseEntity<JsonNode> simulation = post("/api/simulations/shipments?count=25", null);
        assertThat(simulation.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(simulation.getBody().path("data").path("processedCount").asInt()).isEqualTo(25);

        ResponseEntity<JsonNode> summary = restTemplate.getForEntity(url("/api/operations/summary"), JsonNode.class);
        assertThat(summary.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = summary.getBody().path("data");
        assertThat(data.path("service").asText()).isEqualTo("Archive-Logistics");
        assertThat(data.path("receivedEvents").asLong()).isEqualTo(25);
        assertThat(data.path("outbox").path("pending").asLong()).isEqualTo(25);
        assertThat(data.path("ledger").path("enabled").asBoolean()).isFalse();
        assertThat(data.path("ledger").path("contractMode").asText()).isEqualTo("LOGISTICS_CONFIRMED_NATIVE");
    }

    private ResponseEntity<JsonNode> post(String path, Object body) {
        return restTemplate.postForEntity(url(path), body, JsonNode.class);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private Map<String, Object> event(String suffix, String origin, String destination, String priority, boolean coldChain) {
        String shipmentId = "SHIP-IT-" + suffix;
        return Map.of(
                "eventId", "evt-nexus-it-" + suffix,
                "idempotencyKey", "NEXUS:LOGISTICS_DISPATCHED:" + origin + ":" + shipmentId,
                "source", "Archive-Nexus",
                "eventType", "LOGISTICS_DISPATCHED",
                "occurredAt", Instant.parse("2026-01-15T10:32:15Z").toString(),
                "payload", Map.of(
                        "factoryId", origin,
                        "shipmentId", shipmentId,
                        "originCode", origin,
                        "destinationCode", destination,
                        "priority", priority,
                        "itemType", "battery-module",
                        "quantity", 120,
                        "requiresColdChain", coldChain
                )
        );
    }
}
