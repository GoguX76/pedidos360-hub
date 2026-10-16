package com.hub.pedidos360.admin.service;

import com.hub.pedidos360.admin.dto.AdminActionResponse;
import com.hub.pedidos360.admin.dto.BindingRequest;
import com.hub.pedidos360.admin.dto.ExchangeRequest;
import com.hub.pedidos360.admin.dto.QueueRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.HeadersExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class RabbitAdminService {

    private static final Logger log = LoggerFactory.getLogger(RabbitAdminService.class);

    private static final String TYPE_QUEUE = "QUEUE";
    private static final String TYPE_EXCHANGE = "EXCHANGE";
    private static final String TYPE_BINDING = "BINDING";

    private static final String X_DEAD_LETTER_EXCHANGE = "x-dead-letter-exchange";
    private static final String X_DEAD_LETTER_ROUTING_KEY = "x-dead-letter-routing-key";

    private final RabbitAdmin rabbitAdmin;

    public RabbitAdminService(RabbitAdmin rabbitAdmin) {
        this.rabbitAdmin = rabbitAdmin;
    }

    public AdminActionResponse createQueue(QueueRequest request) {
        Map<String, Object> arguments = new HashMap<>();
        if (request.getDeadLetterExchange() != null && !request.getDeadLetterExchange().isBlank()) {
            arguments.put(X_DEAD_LETTER_EXCHANGE, request.getDeadLetterExchange());
        }
        if (request.getDeadLetterRoutingKey() != null && !request.getDeadLetterRoutingKey().isBlank()) {
            arguments.put(X_DEAD_LETTER_ROUTING_KEY, request.getDeadLetterRoutingKey());
        }

        Queue queue = new Queue(
                request.getName(),
                request.isDurable(),
                request.isExclusive(),
                request.isAutoDelete(),
                arguments);

        rabbitAdmin.declareQueue(queue);
        log.info("Cola '{}' creada (durable={}, exclusive={}, autoDelete={})",
                request.getName(), request.isDurable(), request.isExclusive(), request.isAutoDelete());

        return build(true, "Cola creada correctamente", TYPE_QUEUE, request.getName());
    }

    public AdminActionResponse deleteQueue(String name) {
        boolean deleted = rabbitAdmin.deleteQueue(name);
        log.info("Cola '{}' eliminada (resultado={})", name, deleted);
        return build(true, "Cola eliminada correctamente", TYPE_QUEUE, name);
    }

    public AdminActionResponse purgeQueue(String name) {
        rabbitAdmin.purgeQueue(name, false);
        log.info("Cola '{}' purgada (mensajes acumulados eliminados)", name);
        return build(true, "Cola purgada correctamente", TYPE_QUEUE, name);
    }

    public AdminActionResponse createExchange(ExchangeRequest request) {
        Exchange exchange = buildExchange(request);
        rabbitAdmin.declareExchange(exchange);
        log.info("Exchange '{}' creado (tipo={}, durable={}, autoDelete={})",
                request.getName(), request.getType(), request.isDurable(), request.isAutoDelete());

        return build(true, "Exchange creado correctamente", TYPE_EXCHANGE, request.getName());
    }

    public AdminActionResponse deleteExchange(String name) {
        boolean deleted = rabbitAdmin.deleteExchange(name);
        log.info("Exchange '{}' eliminado (resultado={})", name, deleted);
        return build(true, "Exchange eliminado correctamente", TYPE_EXCHANGE, name);
    }

    public AdminActionResponse createBinding(BindingRequest request) {
        Binding binding = buildBinding(request);
        rabbitAdmin.declareBinding(binding);
        log.info("Binding creado: cola '{}' -> exchange '{}' con routingKey '{}'",
                request.getQueueName(), request.getExchangeName(), request.getRoutingKey());

        return build(true, "Binding creado correctamente", TYPE_BINDING,
                request.getQueueName() + " -> " + request.getExchangeName());
    }

    public AdminActionResponse deleteBinding(BindingRequest request) {
        Binding binding = buildBinding(request);
        rabbitAdmin.removeBinding(binding);
        log.info("Binding eliminado: cola '{}' -> exchange '{}' con routingKey '{}'",
                request.getQueueName(), request.getExchangeName(), request.getRoutingKey());

        return build(true, "Binding eliminado correctamente", TYPE_BINDING,
                request.getQueueName() + " -> " + request.getExchangeName());
    }

    private Exchange buildExchange(ExchangeRequest request) {
        String type = request.getType() == null ? "" : request.getType().toLowerCase();
        return switch (type) {
            case "direct" -> new DirectExchange(request.getName(), request.isDurable(), request.isAutoDelete());
            case "topic" -> new TopicExchange(request.getName(), request.isDurable(), request.isAutoDelete());
            case "fanout" -> new FanoutExchange(request.getName(), request.isDurable(), request.isAutoDelete());
            case "headers" -> new HeadersExchange(request.getName(), request.isDurable(), request.isAutoDelete());
            default -> throw new IllegalArgumentException(
                    "Tipo de exchange no soportado: '" + request.getType() + "'. Valores permitidos: direct, topic, fanout, headers");
        };
    }

    private Binding buildBinding(BindingRequest request) {
        return new Binding(
                request.getQueueName(),
                Binding.DestinationType.QUEUE,
                request.getExchangeName(),
                request.getRoutingKey(),
                null);
    }

    private AdminActionResponse build(boolean success, String message, String resourceType, String resourceName) {
        return AdminActionResponse.builder()
                .success(success)
                .message(message)
                .resourceType(resourceType)
                .resourceName(resourceName)
                .timestamp(Instant.now())
                .build();
    }
}
