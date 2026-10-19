package com.hub.pedidos360.orders.messaging;

public interface EventPublisher {
    void publish(String exchange, String routingKey, Object event);
}
