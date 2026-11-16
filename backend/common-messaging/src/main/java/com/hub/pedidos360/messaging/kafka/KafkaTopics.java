package com.hub.pedidos360.messaging.kafka;

/**
 * Nombres de topicos, grupos de consumidores y sufijos del flujo Kafka.
 *
 * <p>Se centralizan aqui para que los productores y consumidores de
 * {@code orders} y {@code products} no repitan literales y para que
 * {@code kafka-admin} pueda crear los mismos topicos al arrancar.</p>
 */
public final class KafkaTopics {

    /** Eventos de pedido publicados por el microservicio orders. */
    public static final String ORDER_EVENTS = "pedidos.eventos";

    /** Alertas de inventario publicadas por el microservicio products. */
    public static final String INVENTORY_ALERTS = "inventario.alertas";

    /** Sufijo con el que se nombran los topicos de mensajes fallidos (DLT). */
    public static final String DEAD_LETTER_SUFFIX = ".DLT";

    public static final String ORDER_EVENTS_DLT = ORDER_EVENTS + DEAD_LETTER_SUFFIX;
    public static final String INVENTORY_ALERTS_DLT = INVENTORY_ALERTS + DEAD_LETTER_SUFFIX;

    /** Grupo que consume alertas de inventario desde orders (notificaciones al local). */
    public static final String ORDERS_NOTIFICATION_GROUP = "orders-notifications";

    /** Grupo que consume eventos de pedido desde products (notificaciones de pedido). */
    public static final String PRODUCTS_ORDER_EVENTS_GROUP = "products-order-events";

    private KafkaTopics() {
    }
}
