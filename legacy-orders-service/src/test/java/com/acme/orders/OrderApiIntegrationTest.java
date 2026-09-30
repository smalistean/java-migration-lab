package com.acme.orders;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full stack: security, controller, service, Hibernate, JSON. Exists because the sliced
 * tests stayed green while reads over HTTP were failing on lazy associations.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrderApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createdOrderCanBeReadBackThroughEveryReadEndpoint() throws Exception {
        mockMvc.perform(post("/api/orders").with(httpBasic("operator", "operator"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerRef": "ACME-IT", "lines": [{"sku": "WIDGET-9", "qty": 2}]}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber());

        mockMvc.perform(get("/api/orders").with(httpBasic("viewer", "viewer")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lines[0].sku").value("WIDGET-9"));

        mockMvc.perform(get("/api/orders/").param("customerRef", "ACME-IT").with(httpBasic("viewer", "viewer")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lines[0].sku").value("WIDGET-9"));

        mockMvc.perform(get("/api/orders/search").param("skuPrefix", "WIDGET").with(httpBasic("viewer", "viewer")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerRef").value("ACME-IT"));
    }

    @Test
    void writesRequireTheOperatorRole() throws Exception {
        mockMvc.perform(post("/api/orders").with(httpBasic("viewer", "viewer"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerRef": "ACME-IT", "lines": []}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/api/orders")).andExpect(status().isUnauthorized());
    }
}
