package com.hub.pedidos360.messaging;

/**
 * Fuente unica de nombres de la topologia de mensajeria (exchanges, colas y
 * routing keys) compartida por los microservicios de negocio.
 *
 * <p>Antes cada servicio declaraba su propia copia de estos nombres; ahora se
 * importan desde este modulo comun para evitar divergencias entre orders y
 * products. El microservicio {@code rabbitmq-admin} no participa: es un
 * inspector del broker que administra exchanges, colas y bindings por nombre en
 * runtime, sin declarar la topologia.</p>
 */
public final class MessagingTopology {

    private MessagingTopology() {
    }

    // --- Exchanges ---
    public static final String EVENTS_EXCHANGE = "pedidos360.events";
    public static final String INVENTORY_EXCHANGE = "pedidos360.inventory";
    public static final String DEAD_LETTER_EXCHANGE = "pedidos360.dlx";

    // --- Colas de negocio ---
    public static final String ORDER_CREATED_QUEUE = "q.order-created";
    public static final String ORDER_STATUS_CHANGED_QUEUE = "q.order-status-changed";
    public static final String STOCK_LOW_QUEUE = "q.stock-low";

    // --- Routing keys ---
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";
    public static final String ORDER_STATUS_CHANGED_ROUTING_KEY = "order.status.changed";
    public static final String STOCK_LOW_ROUTING_KEY = "stock.low";

    // --- Colas muertas (una por cola de negocio) ---
    private static final String DEAD_LETTER_SUFFIX = ".dlq";

    public static final String ORDER_CREATED_DLQ = ORDER_CREATED_QUEUE + DEAD_LETTER_SUFFIX;
    public static final String ORDER_STATUS_CHANGED_DLQ = ORDER_STATUS_CHANGED_QUEUE + DEAD_LETTER_SUFFIX;
    public static final String STOCK_LOW_DLQ = STOCK_LOW_QUEUE + DEAD_LETTER_SUFFIX;

    /** Nombre de la cola muerta asociada a una cola de negocio. */
    public static String deadLetterQueueFor(String queueName) {
        return queueName + DEAD_LETTER_SUFFIX;
    }
}
