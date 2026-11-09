package com.hub.pedidos360.kafka_admin.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.hub.pedidos360.kafka_admin.dto.CreateTopicRequest;
import com.hub.pedidos360.kafka_admin.exception.KafkaAdminException;
import com.hub.pedidos360.kafka_admin.exception.TopicAlreadyExistsException;
import com.hub.pedidos360.kafka_admin.service.KafkaTopicService;

/** Crea al arrancar los topicos definidos en application.yml (infraestructura replicable). */
@Component
class TopicBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TopicBootstrap.class);

    private final KafkaAdminProperties props;
    private final KafkaTopicService topicService;

    TopicBootstrap(KafkaAdminProperties props, KafkaTopicService topicService) {
        this.props = props;
        this.topicService = topicService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!props.bootstrapEnabled()) {
            return;
        }
        for (KafkaAdminProperties.TopicSpec spec : props.topics()) {
            try {
                topicService.create(new CreateTopicRequest(
                        spec.name(), spec.partitions(), spec.replicationFactor(), spec.configs()));
                log.info("Topico '{}' creado", spec.name());
            } catch (TopicAlreadyExistsException e) {
                log.info("Topico '{}' ya existia, se conserva", spec.name());
            } catch (KafkaAdminException e) {
                log.warn("No se pudo crear el topico '{}': {}", spec.name(), e.getMessage());
            }
        }
    }
}
