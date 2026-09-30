package com.acme.orders.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties(prefix = "acme.orders")
@ConstructorBinding
public class AppProperties {

    private final String pricingBaseUrl;
    private final Duration pricingTimeout;
    private final int maxLinesPerOrder;
    private final String reportDirectory;

    public AppProperties(String pricingBaseUrl, Duration pricingTimeout,
                         int maxLinesPerOrder, String reportDirectory) {
        this.pricingBaseUrl = pricingBaseUrl;
        this.pricingTimeout = pricingTimeout == null ? Duration.ofSeconds(5) : pricingTimeout;
        this.maxLinesPerOrder = maxLinesPerOrder == 0 ? 50 : maxLinesPerOrder;
        this.reportDirectory = reportDirectory;
    }

    public String getPricingBaseUrl() { return pricingBaseUrl; }
    public Duration getPricingTimeout() { return pricingTimeout; }
    public int getMaxLinesPerOrder() { return maxLinesPerOrder; }
    public String getReportDirectory() { return reportDirectory; }
}
