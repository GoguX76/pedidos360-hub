package com.hub.pedidos360.orders.config;

import com.hub.pedidos360.messaging.kafka.InventoryAlertEvent;
import com.hub.pedidos360.messaging.kafka.KafkaTopics;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuracion Kafka del microservicio orders.
 *
 * <p>Publica {@link com.hub.pedidos360.messaging.kafka.OrderEvent} en
 * {@code pedidos.eventos} y consume {@link InventoryAlertEvent} desde
 * {@code inventario.alertas}. RabbitMQ sigue siendo el transporte principal
 * de pedidos; Kafka se agrega para el flujo de notificaciones.</p>
 */
@Configuration
public class KafkaConfig {

    private final String bootstrapServers;
    private final String consumerGroup;
    private final int maxAttempts;

    public KafkaConfig(
            @Value("${spring.kafka.bootstrap-servers:localhost:9092,localhost:9093}") String bootstrapServers,
            @Value("${app.kafka.orders-consumer-group:orders-notifications}") String consumerGroup,
            @Value("${app.kafka.retry.max-attempts:2}") int maxAttempts) {
        this.bootstrapServers = bootstrapServers;
        this.consumerGroup = consumerGroup;
        this.maxAttempts = Math.max(0, maxAttempts);
    }

    @Bean
    public ProducerFactory<String, Object> kafkaProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);

        return new DefaultKafkaProducerFactory<>(
                props,
                new StringSerializer(),
                new JacksonJsonSerializer<Object>().noTypeInfo());
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public ConsumerFactory<String, InventoryAlertEvent> inventoryAlertConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroup);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        JacksonJsonDeserializer<InventoryAlertEvent> delegate =
                new JacksonJsonDeserializer<>(InventoryAlertEvent.class).ignoreTypeHeaders();

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(delegate));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, InventoryAlertEvent>
            inventoryAlertListenerContainerFactory(
                    ConsumerFactory<String, InventoryAlertEvent> inventoryAlertConsumerFactory,
                    KafkaTemplate<String, Object> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<String, InventoryAlertEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(inventoryAlertConsumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler(kafkaTemplate));
        return factory;
    }

    /**
     * Reintenta los errores recuperables y publica en {@code <topico>.DLT}
     * cuando se agotan los intentos. Los eventos invalidos no se reintentan:
     * van directo al topico de fallidos.
     */
    private DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> new TopicPartition(
                        record.topic() + KafkaTopics.DEAD_LETTER_SUFFIX, -1));

        DefaultErrorHandler handler = new DefaultErrorHandler(
                recoverer, new FixedBackOff(1_000L, maxAttempts));
        handler.addNotRetryableExceptions(IllegalArgumentException.class);
        handler.setCommitRecovered(true);
        return handler;
    }
}
