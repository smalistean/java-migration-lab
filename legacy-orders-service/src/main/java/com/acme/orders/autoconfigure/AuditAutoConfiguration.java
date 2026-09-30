package com.acme.orders.autoconfigure;

import com.acme.orders.web.RequestAuditFilter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Shipped by the platform team's shared library. Registered via spring.factories.
 */
@Configuration
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "acme.audit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AuditAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RequestAuditFilter requestAuditFilter() {
        return new RequestAuditFilter();
    }
}
