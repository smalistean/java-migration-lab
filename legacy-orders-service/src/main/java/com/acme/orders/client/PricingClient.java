package com.acme.orders.client;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import com.acme.orders.config.AppProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class PricingClient {

    private static final Logger log = LoggerFactory.getLogger(PricingClient.class);

    private final RestTemplate restTemplate;
    private final AppProperties properties;

    public PricingClient(RestTemplateBuilder builder, AppProperties properties) {
        this.properties = properties;
        this.restTemplate = builder
                .setConnectTimeout(properties.getPricingTimeout())
                .setReadTimeout(properties.getPricingTimeout())
                .rootUri(properties.getPricingBaseUrl())
                .build();
    }

    @SuppressWarnings("unchecked")
    public BigDecimal priceFor(String sku) {
        String url = UriComponentsBuilder.fromPath("/prices/{sku}").buildAndExpand(sku).toUriString();
        try {
            Map<String, Object> body = restTemplate.getForObject(url, Map.class);
            if (body == null || !body.containsKey("amount")) {
                return BigDecimal.ZERO;
            }
            return new BigDecimal(String.valueOf(body.get("amount")));
        } catch (RestClientException e) {
            log.warn("Pricing lookup failed for {}: {}", sku, e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    public Map<String, BigDecimal> priceAll() {
        return Collections.emptyMap();
    }
}
