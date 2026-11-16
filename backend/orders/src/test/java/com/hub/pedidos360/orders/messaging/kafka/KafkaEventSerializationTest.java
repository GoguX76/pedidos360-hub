package com.hub.pedidos360.orders.messaging.kafka;

import com.hub.pedidos360.messaging.kafka.InventoryAlertEvent;
import com.hub.pedidos360.messaging.kafka.OrderEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprueba que los DTO compartidos se serializan y deserializan con los
 * serializadores Jackson 3 de Spring Kafka, sin depender de un broker.
 */
class KafkaEventSerializationTest {

    @Test
    @DisplayName("OrderEvent debe sobrevivir un viaje de ida y vuelta por JSON")
    void orderEventRoundTrip() {
        OrderEvent event = new OrderEvent(
                "evt-1",
                OrderEvent.TYPE_CREATED,
                10L,
                "cust-1",
                "Juan",
                2L,
                "DELIVERY",
                "CREATED",
                new BigDecimal("15980.00"),
                List.of(new OrderEvent.Item(5L, "Hamburguesa", 2, new BigDecimal("7990.00"))),
                "2026-11-12T20:00:00");

        byte[] json = serializer().serialize("pedidos.eventos", event);
        OrderEvent recovered = deserializer(OrderEvent.class).deserialize("pedidos.eventos", json);

        assertThat(recovered).isEqualTo(event);
    }

    @Test
    @DisplayName("InventoryAlertEvent debe sobrevivir un viaje de ida y vuelta por JSON")
    void inventoryAlertEventRoundTrip() {
        InventoryAlertEvent event = new InventoryAlertEvent(
                "evt-2",
                5L,
                "Hamburguesa",
                3,
                10,
                "2026-11-12T20:05:00");

        byte[] json = serializer().serialize("inventario.alertas", event);
        InventoryAlertEvent recovered = deserializer(InventoryAlertEvent.class)
                .deserialize("inventario.alertas", json);

        assertThat(recovered).isEqualTo(event);
    }

    private JacksonJsonSerializer<Object> serializer() {
        return new JacksonJsonSerializer<Object>().noTypeInfo();
    }

    private <T> JacksonJsonDeserializer<T> deserializer(Class<T> type) {
        return new JacksonJsonDeserializer<>(type).ignoreTypeHeaders();
    }
}
