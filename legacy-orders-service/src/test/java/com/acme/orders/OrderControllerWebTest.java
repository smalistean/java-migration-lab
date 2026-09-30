package com.acme.orders;

import java.util.Arrays;

import com.acme.orders.domain.PurchaseOrder;
import com.acme.orders.service.OrderService;
import com.acme.orders.web.OrderController;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OrderController.class)
class OrderControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Test
    @WithMockUser(roles = "VIEWER")
    void listReturnsOrders() throws Exception {
        PurchaseOrder order = new PurchaseOrder();
        order.setId(1L);
        order.setCustomerRef("ACME-1");
        when(orderService.find(any())).thenReturn(Arrays.asList(order));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerRef").value("ACME-1"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void listAlsoWorksWithTrailingSlash() throws Exception {
        when(orderService.find(any())).thenReturn(Arrays.asList());

        // the mobile client has always sent the trailing slash
        mockMvc.perform(get("/api/orders/"))
                .andExpect(status().isOk());
    }
}
