package com.hub.pedidos360.products.notification;

import com.hub.pedidos360.messaging.kafka.OrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * Notificacion simulada de pedidos.
 *
 * <p>Por ahora se escribe en el log para que la demostracion sea verificable
 * sin depender de un proveedor externo. No descuenta stock: ese paso sigue
 * siendo responsabilidad del consumidor RabbitMQ.</p>
 */
@Service
public class OrderNotificationService {

    private static final Logger log = LoggerFactory.getLogger(OrderNotificationService.class);

    public void notifyOrderEvent(OrderEvent event) {
        String items = event.items() == null
                ? "sin items"
                : event.items().stream()
                        .map(item -> item.quantity() + "x " + item.productName()
                                + " (ID " + item.productId() + ")")
                        .collect(Collectors.joining(", "));

        log.info("[NOTIFICACION - PRODUCTOS] Evento {} del pedido {} (cliente {}, local {}): "
                        + "estado {}, despacho {}, total {}, items [{}]. Evento {}.",
                event.eventType(),
                event.orderId(),
                event.customerId(),
                event.branchId(),
                event.status(),
                event.dispatchType(),
                event.totalAmount(),
                items,
                event.eventId());
    }
}
