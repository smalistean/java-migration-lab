package com.acme.orders;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import com.acme.orders.client.PricingClient;
import com.acme.orders.config.AppProperties;
import com.acme.orders.domain.OrderLine;
import com.acme.orders.domain.PurchaseOrder;
import com.acme.orders.repo.OrderRepository;
import com.acme.orders.service.OrderService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

class OrderServiceTest {

    private OrderRepository repository;
    private PricingClient pricingClient;
    private OrderService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(OrderRepository.class);
        pricingClient = Mockito.mock(PricingClient.class);
        AppProperties props = new AppProperties("http://localhost:9090", null, 50, "/tmp/acme-reports");
        service = new OrderService(repository, pricingClient, props);
    }

    @Test
    void createComputesTotalFromPricingService() {
        Mockito.when(pricingClient.priceFor(anyString())).thenReturn(new BigDecimal("12.50"));
        Mockito.when(repository.save(any(PurchaseOrder.class))).thenAnswer(i -> i.getArgument(0));

        PurchaseOrder order = new PurchaseOrder();
        order.setCustomerRef("ACME-1");
        OrderLine line = new OrderLine();
        line.setSku("WIDGET-1");
        line.setQty(4);
        order.addLine(line);

        PurchaseOrder saved = service.create(order);

        assertThat(saved.getTotalAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    void findWithBlankCustomerReturnsAll() {
        Mockito.when(repository.findAll()).thenReturn(Collections.emptyList());
        assertThat(service.find("  ")).isEmpty();
        Mockito.verify(repository).findAll();
    }

    @Test
    void findByCustomerDelegatesToRepository() {
        PurchaseOrder o = new PurchaseOrder();
        Mockito.when(repository.findByCustomerRef("ACME-1")).thenReturn(Arrays.asList(o));
        assertThat(service.find("ACME-1")).hasSize(1);
    }
}
