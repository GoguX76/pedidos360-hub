package com.hub.pedidos360.messaging.kafka;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

/**
 * Evento de pedido publicado en el topico {@code pedidos.eventos}.
 *
 * <p>Es un contrato independiente de las entidades JPA: solo contiene datos
 * que los demas servicios necesitan para reaccionar al pedido. Las fechas se
 * envian como texto ISO-8601 para no acoplar la serializacion a una version
 * concreta de Jackson.</p>
 */
public record OrderEvent(
        @NotBlank(message = "el id del evento es obligatorio")
        String eventId,

        @NotBlank(message = "el tipo de evento es obligatorio")
        String eventType,

        @NotNull(message = "el id del pedido es obligatorio")
        Long orderId,

        @NotBlank(message = "el id del cliente es obligatorio")
        String customerId,

        @NotBlank(message = "el nombre del cliente es obligatorio")
        String customerName,

        @NotNull(message = "el id del local es obligatorio")
        Long branchId,

        @NotBlank(message = "el tipo de despacho es obligatorio")
        String dispatchType,

        @NotBlank(message = "el estado del pedido es obligatorio")
        String status,

        @NotNull(message = "el total es obligatorio")
        @Positive(message = "el total debe ser mayor a 0")
        BigDecimal totalAmount,

        @NotEmpty(message = "el pedido debe tener al menos un item")
        @Valid
        List<Item> items,

        @NotBlank(message = "la fecha del evento es obligatoria")
        String occurredAt) {

    public static final String TYPE_CREATED = "ORDER_CREATED";
    public static final String TYPE_STATUS_CHANGED = "ORDER_STATUS_CHANGED";

    /** Item del pedido tal como viaja en el evento (sin referencias JPA). */
    public record Item(
            @NotNull(message = "el id del producto es obligatorio")
            Long productId,

            @NotBlank(message = "el nombre del producto es obligatorio")
            String productName,

            @NotNull(message = "la cantidad es obligatoria")
            @Positive(message = "la cantidad debe ser mayor a 0")
            Integer quantity,

            @NotNull(message = "el precio unitario es obligatorio")
            BigDecimal unitPrice) {
    }
}
