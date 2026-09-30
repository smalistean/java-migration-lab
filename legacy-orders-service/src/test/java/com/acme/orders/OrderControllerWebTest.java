package com.acme.orders;

import java.util.Arrays;

import com.acme.orders.config.SecurityConfig;
import com.acme.orders.domain.OrderLine;
import com.acme.orders.domain.PurchaseOrder;
import com.acme.orders.service.OrderService;
import com.acme.orders.web.OrderController;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OrderController.class)
@Import(SecurityConfig.class)
class OrderControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    @WithMockUser(roles = "VIEWER")
    void listReturnsOrders() throws Exception {
        PurchaseOrder order = new PurchaseOrder();
        order.setId(1L);
        order.setCustomerRef("ACME-1");
        OrderLine line = new OrderLine();
        line.setSku("W-1");
        order.addLine(line);
        when(orderService.find(any())).thenReturn(Arrays.asList(order));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerRef").value("ACME-1"))
                .andExpect(jsonPath("$[0].lines[0].sku").value("W-1"))
                .andExpect(jsonPath("$[0].lines[0].order").doesNotExist());
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
