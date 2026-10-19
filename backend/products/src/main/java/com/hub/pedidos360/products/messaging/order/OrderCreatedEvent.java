package com.hub.pedidos360.products.messaging.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "El id del pedido es obligatorio")
    private Long orderId;

    @NotBlank(message = "El id del cliente es obligatorio")
    private String customerId;

    @NotNull(message = "El id del local es obligatorio")
    private Long branchId;

    @NotEmpty(message = "El pedido debe tener al menos un item")
    @Valid
    private List<OrderItemEvent> items;

    @NotNull(message = "El total es obligatorio")
    @Positive(message = "El total debe ser mayor a 0")
    private BigDecimal totalAmount;

    @NotNull(message = "La fecha de creación es obligatoria")
    private LocalDateTime createdAt;
}
