package com.acme.orders.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.UrlHandlerFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * Spring 6 no longer matches "/api/orders/" against "/api/orders". Existing clients
     * (including the mobile app) send the trailing slash, so it is handled explicitly
     * here instead of relying on the removed setUseTrailingSlashMatch switch.
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public UrlHandlerFilter trailingSlashFilter() {
        return UrlHandlerFilter.trailingSlashHandler("/api/**").wrapRequest().build();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("https://ops.acme.example")
                .allowedMethods("GET", "POST", "PUT", "DELETE");
    }
}
