package com.csj.archive.logistics.settlement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class NexusDailySettlementClient {
    private final RestClient.Builder restClientBuilder;
    private final NexusSettlementProperties properties;
    private final ObjectMapper objectMapper;

    public NexusDailySettlementClient(RestClient.Builder restClientBuilder,
                                      NexusSettlementProperties properties,
                                      ObjectMapper objectMapper) {
        this.restClientBuilder = restClientBuilder;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public JsonNode publish(NexusDailySettlementRequest request) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.getPublishTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.getPublishTimeoutMs()));

        RestClient client = restClientBuilder
                .clone()
                .requestFactory(requestFactory)
                .baseUrl(properties.getBaseUrl())
                .build();

        String responseBody = client.post()
                .uri(properties.getDailyEndpoint())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange((httpRequest, response) -> {
                    String rawBody = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        throw new IllegalStateException("Nexus daily settlement publish failed with status "
                                + response.getStatusCode() + ": " + rawBody);
                    }
                    return rawBody;
                });

        if (responseBody == null || responseBody.isBlank()) {
            return objectMapper.createObjectNode().put("status", "ACCEPTED");
        }
        try {
            return objectMapper.readTree(responseBody);
        } catch (Exception error) {
            throw new IllegalStateException("Nexus daily settlement response parsing failed: " + responseBody, error);
        }
    }
}
