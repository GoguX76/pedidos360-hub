package com.hub.pedidos360.orders.config;

import com.hub.pedidos360.messaging.RabbitTopologyConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Habilita la topologia de mensajeria compartida (exchanges, colas, bindings y
 * conversor JSON) sin duplicar la declaracion en cada servicio.
 */
@Configuration
@Import(RabbitTopologyConfig.class)
public class MessagingConfig {
}
