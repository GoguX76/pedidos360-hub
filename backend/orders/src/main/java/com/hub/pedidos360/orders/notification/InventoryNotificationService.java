package com.hub.pedidos360.orders.notification;

import com.hub.pedidos360.messaging.kafka.InventoryAlertEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Notificacion simulada de inventario.
 *
 * <p>Por ahora se escribe en el log para que la demostracion sea verificable
 * sin depender de un proveedor externo. Si mas adelante se exige correo, SMS
 * o notificacion web, solo se reemplaza el cuerpo de este metodo.</p>
 */
@Service
public class InventoryNotificationService {

    private static final Logger log = LoggerFactory.getLogger(InventoryNotificationService.class);

    public void notifyLowStock(InventoryAlertEvent event) {
        log.warn("[NOTIFICACION - PEDIDOS] Stock critico del producto '{}' (ID {}): "
                        + "quedan {} unidades (umbral {}). Evento {}.",
                event.productName(),
                event.productId(),
                event.currentStock(),
                event.threshold(),
                event.eventId());
    }
}
