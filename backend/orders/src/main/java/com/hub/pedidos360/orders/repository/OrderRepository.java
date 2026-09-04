package com.hub.pedidos360.orders.repository;

import com.hub.pedidos360.orders.model.DispatchType;
import com.hub.pedidos360.orders.model.Order;
import com.hub.pedidos360.orders.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerId(String customerId);
    List<Order> findByBranchId(Long branchId);
    List<Order> findByStatus(OrderStatus status);
    List<Order> findByDispatchType(DispatchType dispatchType);
}
