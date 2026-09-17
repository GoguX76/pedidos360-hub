package com.hub.pedidos360.orders.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    @NotBlank(message = "El nombre del cliente es obligatorio")
    private String customerName;

    @NotNull(message = "El ID del local es obligatorio")
    private Long branchId;

    @NotBlank(message = "El tipo de despacho es obligatorio")
    private String dispatchType;

    @NotEmpty(message = "El pedido debe tener al menos un item")
    @Valid
    private List<OrderItemRequest> items;
}
