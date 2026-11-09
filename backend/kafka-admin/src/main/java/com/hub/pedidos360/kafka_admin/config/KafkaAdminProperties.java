package com.hub.pedidos360.kafka_admin.config;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kafka-admin")
public record KafkaAdminProperties(int timeoutSeconds, boolean bootstrapEnabled, List<TopicSpec> topics) {

    public KafkaAdminProperties {
        if (timeoutSeconds <= 0) {
            timeoutSeconds = 10;
        }
        topics = topics == null ? List.of() : topics;
    }

    public record TopicSpec(String name, int partitions, short replicationFactor, Map<String, String> configs) {
        public TopicSpec {
            configs = configs == null ? Map.of() : configs;
        }
    }
}
