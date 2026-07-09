package com.csj.archive.logistics.ledger;

import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Component
public class LedgerPublisherClient {
    private final RestClient.Builder restClientBuilder;
    private final LedgerPublishProperties properties;
    private final ObjectMapper objectMapper;

    public LedgerPublisherClient(RestClient.Builder restClientBuilder,
                                 LedgerPublishProperties properties,
                                 ObjectMapper objectMapper) {
        this.restClientBuilder = restClientBuilder;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public LedgerBulkPublishResponse publish(List<LedgerCompatibleEventPayload> events) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.getPublishTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.getPublishTimeoutMs()));

        RestClient client = restClientBuilder
                .clone()
                .requestFactory(requestFactory)
                .baseUrl(properties.getBaseUrl())
                .build();

        Object body = properties.getContractMode() == LedgerContractMode.ARCHIVE_LEDGER_V1_COMPAT
                ? events
                : new LedgerBulkPublishRequest(events);

        String responseBody = client.post()
                .uri(properties.getBulkEndpoint())
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange((request, response) -> {
                    String rawBody = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        throw new IllegalStateException("Ledger bulk publish failed with status "
                                + response.getStatusCode() + ": " + rawBody);
                    }
                    return rawBody;
                });

        if (responseBody == null || responseBody.isBlank()) {
            return LedgerBulkPublishResponse.success(events.size());
        }
        try {
            JsonNode response = objectMapper.readTree(responseBody);
            JsonNode payload = response.has("data") ? response.path("data") : response;
            return objectMapper.treeToValue(payload, LedgerBulkPublishResponse.class);
        } catch (Exception error) {
            throw new IllegalStateException("Ledger bulk publish response parsing failed: " + responseBody, error);
        }
    }
}
