package com.acme.orders.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.transaction.annotation.Transactional;

import com.acme.orders.client.PricingClient;
import com.acme.orders.config.AppProperties;
import com.acme.orders.domain.OrderLine;
import com.acme.orders.domain.OrderStatus;
import com.acme.orders.domain.PurchaseOrder;
import com.acme.orders.repo.OrderRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    // immutable and thread-safe; numeric month and fixed zone so output does not vary by host or JDK
    private static final DateTimeFormatter AUDIT_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    private final OrderRepository repository;
    private final PricingClient pricingClient;
    private final AppProperties properties;

    public OrderService(OrderRepository repository, PricingClient pricingClient, AppProperties properties) {
        this.repository = repository;
        this.pricingClient = pricingClient;
        this.properties = properties;
    }

    @PostConstruct
    public void warmUp() {
        log.info("OrderService starting at {}", AUDIT_FORMAT.format(Instant.now()));
    }

    @PreDestroy
    public void shutdown() {
        log.info("OrderService stopping at {}", AUDIT_FORMAT.format(Instant.now()));
    }

    public List<PurchaseOrder> find(String customerRef) {
        if (customerRef == null || customerRef.isBlank()) {
            return repository.findAll();
        }
        return repository.findByCustomerRef(customerRef);
    }

    public Optional<PurchaseOrder> findById(Long id) {
        return repository.findById(id);
    }

    public List<PurchaseOrder> searchBySkuPrefix(String prefix) {
        return repository.findBySkuPrefix(prefix);
    }

    @Transactional
    public PurchaseOrder create(PurchaseOrder order) {
        if (order.getLines().size() > properties.getMaxLinesPerOrder()) {
            throw new IllegalArgumentException("Too many lines");
        }
        BigDecimal total = BigDecimal.ZERO;
        for (OrderLine line : order.getLines()) {
            BigDecimal unit = pricingClient.priceFor(line.getSku());
            line.setUnitPrice(unit);
            line.setOrder(order);
            total = total.add(unit.multiply(new BigDecimal(line.getQty())));
        }
        order.setTotalAmount(total);
        order.setStatus(OrderStatus.NEW);
        order.setCreatedAt(Instant.now());
        return repository.save(order);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Scheduled(fixedDelayString = "${acme.orders.sweep-interval-ms:60000}")
    public void sweepStaleOrders() {
        Instant since = Instant.now().minus(Duration.ofDays(30));
        List<PurchaseOrder> stale = repository.findRecentByStatus(OrderStatus.NEW, since);
        log.debug("Sweep found {} stale orders as of {}", stale.size(), AUDIT_FORMAT.format(Instant.now()));
    }
}
