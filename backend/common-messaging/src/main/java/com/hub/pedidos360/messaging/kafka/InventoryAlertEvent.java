package com.hub.pedidos360.messaging.kafka;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Alerta de stock bajo publicada en el topico {@code inventario.alertas}.
 *
 * <p>La produce el microservicio products cuando detecta que un producto
 * quedo en o por debajo del umbral configurado. La consume orders para
 * generar la notificacion correspondiente.</p>
 */
public record InventoryAlertEvent(
        @NotBlank(message = "el id del evento es obligatorio")
        String eventId,

        @NotNull(message = "el id del producto es obligatorio")
        Long productId,

        @NotBlank(message = "el nombre del producto es obligatorio")
        String productName,

        @NotNull(message = "el stock actual es obligatorio")
        @PositiveOrZero(message = "el stock actual no puede ser negativo")
        Integer currentStock,

        @NotNull(message = "el umbral es obligatorio")
        Integer threshold,

        @NotBlank(message = "la fecha de la alerta es obligatoria")
        String alertAt) {
}
