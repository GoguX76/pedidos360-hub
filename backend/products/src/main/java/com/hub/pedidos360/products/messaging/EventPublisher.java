package com.hub.pedidos360.products.messaging;

public interface EventPublisher {
    void publish(String exchange, String routingKey, Object event);
}
