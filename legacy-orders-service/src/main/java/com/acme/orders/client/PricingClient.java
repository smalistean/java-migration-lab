package com.acme.orders.client;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.util.Map;

import com.acme.orders.config.AppProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class PricingClient {

    private static final Logger log = LoggerFactory.getLogger(PricingClient.class);
    private static final ParameterizedTypeReference<Map<String, Object>> JSON_OBJECT = new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    /** The injected builder carries Boot's message converters and tracing instrumentation. */
    public PricingClient(RestClient.Builder builder, AppProperties properties) {
        var requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(properties.getPricingTimeout()).build());
        requestFactory.setReadTimeout(properties.getPricingTimeout());
        this.restClient = builder
                .baseUrl(properties.getPricingBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    public BigDecimal priceFor(String sku) {
        try {
            Map<String, Object> body = restClient.get().uri("/prices/{sku}", sku).retrieve().body(JSON_OBJECT);
            if (body == null || !body.containsKey("amount")) {
                return BigDecimal.ZERO;
            }
            return new BigDecimal(String.valueOf(body.get("amount")));
        } catch (RestClientException e) {
            log.warn("Pricing lookup failed for {}: {}", sku, e.getMessage());
            return BigDecimal.ZERO;
        }
    }
}
