package com.acme.orders.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Entity
@Table(name = "purchase_order")
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull
    @Size(min = 3, max = 64)
    @Column(name = "customerRef", nullable = false)
    private String customerRef;

    @Enumerated(EnumType.ORDINAL)
    private OrderStatus status = OrderStatus.NEW;

    @Column(name = "totalAmount", precision = 19, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "createdAt")
    private Date createdAt = new Date();

    @Column(name = "shippingNotes", length = 4000)
    private String shippingNotes;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLine> lines = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCustomerRef() { return customerRef; }
    public void setCustomerRef(String customerRef) { this.customerRef = customerRef; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public String getShippingNotes() { return shippingNotes; }
    public void setShippingNotes(String shippingNotes) { this.shippingNotes = shippingNotes; }

    public List<OrderLine> getLines() { return lines; }
    public void setLines(List<OrderLine> lines) { this.lines = lines; }

    public void addLine(OrderLine line) {
        lines.add(line);
        line.setOrder(this);
    }
}
