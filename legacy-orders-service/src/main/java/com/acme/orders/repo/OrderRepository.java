package com.acme.orders.repo;

import java.util.Date;
import java.util.List;

import com.acme.orders.domain.OrderStatus;
import com.acme.orders.domain.PurchaseOrder;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<PurchaseOrder, Long> {

    List<PurchaseOrder> findByCustomerRef(String customerRef);

    @Query("select o from PurchaseOrder o where o.status = :status and o.createdAt >= :since")
    List<PurchaseOrder> findRecentByStatus(@Param("status") OrderStatus status, @Param("since") Date since);

    // implicit join across the collection, and a bare alias in the select list
    @Query("select distinct o from PurchaseOrder o, OrderLine l where l.order = o and l.sku like :prefix%")
    List<PurchaseOrder> findBySkuPrefix(@Param("prefix") String prefix);

    @Query("select o.customerRef, sum(o.totalAmount) from PurchaseOrder o group by o.customerRef order by 2 desc")
    List<Object[]> totalsByCustomer();

    @Query(value = "select * from purchase_order where rownum <= :n", nativeQuery = true)
    List<PurchaseOrder> findTopN(@Param("n") int n);
}
