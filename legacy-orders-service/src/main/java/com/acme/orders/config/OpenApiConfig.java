package com.acme.orders.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ordersOpenApi() {
        return new OpenAPI().info(new Info().title("Orders API").version("v1"));
    }

    @Bean
    public GroupedOpenApi ordersApi() {
        return GroupedOpenApi.builder().group("orders").pathsToMatch("/api/**").build();
    }
}
