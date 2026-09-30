package com.acme.orders.repo;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.acme.orders.domain.OrderStatus;
import com.acme.orders.domain.PurchaseOrder;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<PurchaseOrder, Long> {

    // open-in-view is off, so everything the API serialises is fetched here, in one query.

    @Override
    @EntityGraph(attributePaths = "lines")
    List<PurchaseOrder> findAll();

    @Override
    @EntityGraph(attributePaths = "lines")
    Optional<PurchaseOrder> findById(Long id);

    @EntityGraph(attributePaths = "lines")
    List<PurchaseOrder> findByCustomerRef(String customerRef);

    @Query("select o from PurchaseOrder o where o.status = :status and o.createdAt >= :since")
    List<PurchaseOrder> findRecentByStatus(@Param("status") OrderStatus status, @Param("since") Instant since);

    @EntityGraph(attributePaths = "lines")
    @Query("select distinct o from PurchaseOrder o join o.lines l where l.sku like concat(:prefix, '%')")
    List<PurchaseOrder> findBySkuPrefix(@Param("prefix") String prefix);

    @Query("select o.customerRef, sum(o.totalAmount) from PurchaseOrder o group by o.customerRef order by sum(o.totalAmount) desc")
    List<Object[]> totalsByCustomer();

    @Query("select o from PurchaseOrder o order by o.createdAt desc")
    List<PurchaseOrder> findMostRecent(Pageable page);
}
