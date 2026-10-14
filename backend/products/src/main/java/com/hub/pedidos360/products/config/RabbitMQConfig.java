package com.hub.pedidos360.products.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_EVENTS = "pedidos360.events";
    public static final String EXCHANGE_INVENTORY = "pedidos360.inventory";
    public static final String EXCHANGE_DLX = "pedidos360.dlx";

    public static final String QUEUE_ORDER_CREATED = "q.order-created";
    public static final String QUEUE_ORDER_CREATED_DLQ = "q.order-created.dlq";
    public static final String QUEUE_ORDER_STATUS_CHANGED = "q.order-status-changed";
    public static final String QUEUE_ORDER_STATUS_CHANGED_DLQ = "q.order-status-changed.dlq";
    public static final String QUEUE_STOCK_LOW = "q.stock-low";
    public static final String QUEUE_STOCK_LOW_DLQ = "q.stock-low.dlq";

    public static final String ROUTING_ORDER_CREATED = "order.created";
    public static final String ROUTING_ORDER_STATUS_CHANGED = "order.status.changed";
    public static final String ROUTING_STOCK_LOW = "stock.low";

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EXCHANGE_EVENTS, true, false);
    }

    @Bean
    public DirectExchange inventoryExchange() {
        return new DirectExchange(EXCHANGE_INVENTORY, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(EXCHANGE_DLX, true, false);
    }

    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable(QUEUE_ORDER_CREATED)
                .deadLetterExchange(EXCHANGE_DLX)
                .deadLetterRoutingKey(QUEUE_ORDER_CREATED_DLQ)
                .build();
    }

    @Bean
    public Queue orderCreatedDlq() {
        return QueueBuilder.durable(QUEUE_ORDER_CREATED_DLQ).build();
    }

    @Bean
    public Queue orderStatusChangedQueue() {
        return QueueBuilder.durable(QUEUE_ORDER_STATUS_CHANGED)
                .deadLetterExchange(EXCHANGE_DLX)
                .deadLetterRoutingKey(QUEUE_ORDER_STATUS_CHANGED_DLQ)
                .build();
    }

    @Bean
    public Queue orderStatusChangedDlq() {
        return QueueBuilder.durable(QUEUE_ORDER_STATUS_CHANGED_DLQ).build();
    }

    @Bean
    public Queue stockLowQueue() {
        return QueueBuilder.durable(QUEUE_STOCK_LOW)
                .deadLetterExchange(EXCHANGE_DLX)
                .deadLetterRoutingKey(QUEUE_STOCK_LOW_DLQ)
                .build();
    }

    @Bean
    public Queue stockLowDlq() {
        return QueueBuilder.durable(QUEUE_STOCK_LOW_DLQ).build();
    }

    @Bean
    public Binding orderCreatedBinding(Queue orderCreatedQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(orderCreatedQueue).to(eventsExchange).with(ROUTING_ORDER_CREATED);
    }

    @Bean
    public Binding orderStatusChangedBinding(Queue orderStatusChangedQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(orderStatusChangedQueue).to(eventsExchange).with(ROUTING_ORDER_STATUS_CHANGED);
    }

    @Bean
    public Binding stockLowBinding(Queue stockLowQueue,
            @Qualifier("inventoryExchange") DirectExchange inventoryExchange) {
        return BindingBuilder.bind(stockLowQueue).to(inventoryExchange).with(ROUTING_STOCK_LOW);
    }

    @Bean
    public Binding orderCreatedDlqBinding(Queue orderCreatedDlq,
            @Qualifier("deadLetterExchange") DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(orderCreatedDlq).to(deadLetterExchange).with(QUEUE_ORDER_CREATED_DLQ);
    }

    @Bean
    public Binding orderStatusChangedDlqBinding(Queue orderStatusChangedDlq,
            @Qualifier("deadLetterExchange") DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(orderStatusChangedDlq).to(deadLetterExchange).with(QUEUE_ORDER_STATUS_CHANGED_DLQ);
    }

    @Bean
    public Binding stockLowDlqBinding(Queue stockLowDlq,
            @Qualifier("deadLetterExchange") DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(stockLowDlq).to(deadLetterExchange).with(QUEUE_STOCK_LOW_DLQ);
    }

    @Bean
    public JacksonJsonMessageConverter jsonMessageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        converter.setTypePrecedence(JacksonJavaTypeMapper.TypePrecedence.INFERRED);
        return converter;
    }
}
