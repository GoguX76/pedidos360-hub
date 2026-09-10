package com.hub.pedidos360.orders.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    private String customerName;
    private Long branchId;
    private String dispatchType;
    private List<OrderItemRequest> items;
}
