package com.hub.pedidos360.orders.messaging.inventory;

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
public class StockLowEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "El id del producto es obligatorio")
    private Long productId;

    @NotBlank(message = "El nombre del producto es obligatorio")
    private String productName;

    @NotNull(message = "El stock actual es obligatorio")
    private Integer currentStock;

    @NotNull(message = "El umbral es obligatorio")
    private Integer threshold;

    @NotNull(message = "La fecha de alerta es obligatoria")
    private LocalDateTime alertAt;
}
