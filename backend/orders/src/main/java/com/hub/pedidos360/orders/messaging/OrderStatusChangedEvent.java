package com.hub.pedidos360.orders.messaging;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusChangedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "El id del pedido es obligatorio")
    private Long orderId;

    @NotBlank(message = "El id del cliente es obligatorio")
    private String customerId;

    @NotBlank(message = "El estado anterior es obligatorio")
    private String previousStatus;

    @NotBlank(message = "El nuevo estado es obligatorio")
    private String newStatus;

    @NotNull(message = "La fecha de actualización es obligatoria")
    private LocalDateTime updatedAt;
}
