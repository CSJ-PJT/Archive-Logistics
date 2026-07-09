package com.csj.archive.logistics.ledger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class LedgerPublisherClientTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void publishesNativeLedgerBulkRequestAndParsesDirectResponse() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = server("""
                {"received":1,"accepted":1,"duplicate":0,"failed":0,"results":[{"eventId":"evt-1","status":"ACCEPTED","transactionId":"TX-1","duplicate":false,"message":"ok"}]}
                """, requestBody);

        try {
            LedgerBulkPublishResponse response = client(server).publish(List.of(payload("evt-1")));

            assertThat(response.received()).isEqualTo(1);
            assertThat(response.accepted()).isEqualTo(1);
            assertThat(response.failed()).isZero();
            assertThat(response.results()).hasSize(1);
            assertThat(response.results().getFirst().successful()).isTrue();
            assertThat(requestBody.get()).contains("\"source\":\"Archive-Logitics\"");
            assertThat(requestBody.get()).contains("\"events\"");
            assertThat(requestBody.get()).contains("\"eventId\":\"evt-1\"");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void parsesApiResponseWrapperWhenLedgerAddsDataEnvelope() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = server("""
                {"traceId":"trace-test","data":{"received":1,"accepted":0,"duplicate":1,"failed":0,"results":[{"eventId":"evt-2","status":"DUPLICATE","transactionId":null,"duplicate":true,"message":"duplicate"}]}}
                """, requestBody);

        try {
            LedgerBulkPublishResponse response = client(server).publish(List.of(payload("evt-2")));

            assertThat(response.received()).isEqualTo(1);
            assertThat(response.duplicate()).isEqualTo(1);
            assertThat(response.successfulCount()).isEqualTo(1);
            assertThat(response.results().getFirst().successful()).isTrue();
        } finally {
            server.stop(0);
        }
    }

    private LedgerPublisherClient client(HttpServer server) {
        LedgerPublishProperties properties = new LedgerPublishProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("http://localhost:" + server.getAddress().getPort());
        properties.setBulkEndpoint("/api/events/logistics/bulk");
        properties.setContractMode(LedgerContractMode.LOGISTICS_CONFIRMED_NATIVE);
        properties.setPublishTimeoutMs(3000);
        return new LedgerPublisherClient(RestClient.builder(), properties, objectMapper);
    }

    private HttpServer server(String responseJson, AtomicReference<String> requestBody) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/events/logistics/bulk", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = responseJson.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        return server;
    }

    private LedgerCompatibleEventPayload payload(String eventId) {
        return new LedgerCompatibleEventPayload(
                eventId,
                "LOGISTICS:LOGISTICS_COST_CONFIRMED:ROUTE-1",
                "Archive-Logitics",
                "LOGISTICS_COST_CONFIRMED",
                "ROUTE_PLAN",
                "ROUTE-1",
                1,
                null,
                objectMapper.createObjectNode()
                        .put("routePlanId", "ROUTE-1")
                        .put("shipmentId", "SHIP-1")
                        .put("factoryId", "FAC-A")
                        .put("vendorId", "VENDOR-LOGISTICS-01")
                        .put("totalCost", 93420)
                        .put("currency", "KRW")
        );
    }
}
