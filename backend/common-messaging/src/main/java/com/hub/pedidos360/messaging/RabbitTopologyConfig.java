package com.hub.pedidos360.messaging;

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

/**
 * Topologia completa del broker declarada una sola vez y reutilizada por todos
 * los servicios (separada de la logica de negocio).
 *
 * <p>Cada microservicio la habilita con {@code @Import(RabbitTopologyConfig.class)};
 * como vive en un paquete distinto al de la aplicacion, no se toma por
 * component-scan accidental.</p>
 */
@Configuration
public class RabbitTopologyConfig {

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(MessagingTopology.EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange inventoryExchange() {
        return new DirectExchange(MessagingTopology.INVENTORY_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(MessagingTopology.DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable(MessagingTopology.ORDER_CREATED_QUEUE)
                .deadLetterExchange(MessagingTopology.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(MessagingTopology.ORDER_CREATED_DLQ)
                .build();
    }

    @Bean
    public Queue orderCreatedDlq() {
        return QueueBuilder.durable(MessagingTopology.ORDER_CREATED_DLQ).build();
    }

    @Bean
    public Queue orderStatusChangedQueue() {
        return QueueBuilder.durable(MessagingTopology.ORDER_STATUS_CHANGED_QUEUE)
                .deadLetterExchange(MessagingTopology.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(MessagingTopology.ORDER_STATUS_CHANGED_DLQ)
                .build();
    }

    @Bean
    public Queue orderStatusChangedDlq() {
        return QueueBuilder.durable(MessagingTopology.ORDER_STATUS_CHANGED_DLQ).build();
    }

    @Bean
    public Queue stockLowQueue() {
        return QueueBuilder.durable(MessagingTopology.STOCK_LOW_QUEUE)
                .deadLetterExchange(MessagingTopology.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(MessagingTopology.STOCK_LOW_DLQ)
                .build();
    }

    @Bean
    public Queue stockLowDlq() {
        return QueueBuilder.durable(MessagingTopology.STOCK_LOW_DLQ).build();
    }

    @Bean
    public Binding orderCreatedBinding(@Qualifier("orderCreatedQueue") Queue orderCreatedQueue,
            TopicExchange eventsExchange) {
        return BindingBuilder.bind(orderCreatedQueue).to(eventsExchange)
                .with(MessagingTopology.ORDER_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding orderStatusChangedBinding(@Qualifier("orderStatusChangedQueue") Queue orderStatusChangedQueue,
            TopicExchange eventsExchange) {
        return BindingBuilder.bind(orderStatusChangedQueue).to(eventsExchange)
                .with(MessagingTopology.ORDER_STATUS_CHANGED_ROUTING_KEY);
    }

    @Bean
    public Binding stockLowBinding(@Qualifier("stockLowQueue") Queue stockLowQueue,
            @Qualifier("inventoryExchange") DirectExchange inventoryExchange) {
        return BindingBuilder.bind(stockLowQueue).to(inventoryExchange)
                .with(MessagingTopology.STOCK_LOW_ROUTING_KEY);
    }

    @Bean
    public Binding orderCreatedDlqBinding(@Qualifier("orderCreatedDlq") Queue orderCreatedDlq,
            @Qualifier("deadLetterExchange") DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(orderCreatedDlq).to(deadLetterExchange)
                .with(MessagingTopology.ORDER_CREATED_DLQ);
    }

    @Bean
    public Binding orderStatusChangedDlqBinding(@Qualifier("orderStatusChangedDlq") Queue orderStatusChangedDlq,
            @Qualifier("deadLetterExchange") DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(orderStatusChangedDlq).to(deadLetterExchange)
                .with(MessagingTopology.ORDER_STATUS_CHANGED_DLQ);
    }

    @Bean
    public Binding stockLowDlqBinding(@Qualifier("stockLowDlq") Queue stockLowDlq,
            @Qualifier("deadLetterExchange") DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(stockLowDlq).to(deadLetterExchange)
                .with(MessagingTopology.STOCK_LOW_DLQ);
    }

    @Bean
    public JacksonJsonMessageConverter jsonMessageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        converter.setTypePrecedence(JacksonJavaTypeMapper.TypePrecedence.INFERRED);
        return converter;
    }
}
