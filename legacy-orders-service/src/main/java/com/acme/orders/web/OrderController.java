package com.acme.orders.web;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import com.acme.orders.domain.PurchaseOrder;
import com.acme.orders.service.OrderService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/api/orders")
@Validated
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<PurchaseOrder> list(@RequestParam(required = false) String customerRef) {
        return orderService.find(customerRef);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrder> get(@PathVariable Long id) {
        return orderService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<PurchaseOrder> create(@Valid @RequestBody PurchaseOrder order) {
        return ResponseEntity.ok(orderService.create(order));
    }

    @GetMapping("/search")
    public List<PurchaseOrder> search(@RequestParam @NotBlank String skuPrefix) {
        return orderService.searchBySkuPrefix(skuPrefix);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        orderService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
