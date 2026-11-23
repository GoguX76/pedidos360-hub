package com.hub.pedidos360.kafka_admin.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.springframework.stereotype.Service;

import com.hub.pedidos360.kafka_admin.config.KafkaAdminProperties;
import com.hub.pedidos360.kafka_admin.dto.CreateTopicRequest;
import com.hub.pedidos360.kafka_admin.dto.PartitionDetail;
import com.hub.pedidos360.kafka_admin.dto.TopicDetail;
import com.hub.pedidos360.kafka_admin.dto.TopicInfo;
import com.hub.pedidos360.kafka_admin.dto.UpdateTopicRequest;
import com.hub.pedidos360.kafka_admin.exception.InvalidTopicRequestException;

@Service
public class KafkaTopicService {

    /** Configuraciones de topico que este administrador permite modificar. */
    static final Set<String> ALLOWED_CONFIGS = Set.of(
            "retention.ms", "retention.bytes", "cleanup.policy",
            "min.insync.replicas", "segment.bytes", "max.message.bytes", "compression.type");

    private final Admin admin;
    private final int timeout;

    public KafkaTopicService(Admin admin, KafkaAdminProperties props) {
        this.admin = admin;
        this.timeout = props.timeoutSeconds();
    }

    public List<TopicInfo> list() {
        Set<String> names = KafkaCalls.await(
                admin.listTopics(new ListTopicsOptions().listInternal(false)).names(), timeout, "Listar topicos");
        if (names.isEmpty()) {
            return List.of();
        }
        Map<String, TopicDescription> described = KafkaCalls.await(
                admin.describeTopics(names).allTopicNames(), timeout, "Describir topicos");
        return described.values().stream()
                .map(d -> new TopicInfo(d.name(), d.partitions().size(), replication(d), d.isInternal()))
                .sorted((a, b) -> a.name().compareTo(b.name()))
                .toList();
    }

    public TopicDetail get(String name) {
        TopicDescription d = describe(name);
        List<PartitionDetail> partitions = d.partitions().stream().map(this::toPartition).toList();
        return new TopicDetail(d.name(), d.isInternal(), partitions, getConfig(name));
    }

    public TopicInfo create(CreateTopicRequest r) {
        Map<String, String> configs = validateConfigs(r.configs());
        NewTopic topic = new NewTopic(r.name(), r.partitions(), r.replicationFactor()).configs(configs);
        KafkaCalls.await(admin.createTopics(List.of(topic)).all(), timeout, "Crear topico " + r.name());
        return new TopicInfo(r.name(), r.partitions(), r.replicationFactor(), false);
    }

    public TopicDetail update(String name, UpdateTopicRequest r) {
        boolean hasConfigs = r.configs() != null && !r.configs().isEmpty();
        if (r.partitions() == null && !hasConfigs) {
            throw new InvalidTopicRequestException("Indica 'partitions' y/o 'configs' para actualizar");
        }
        Map<String, String> configs = hasConfigs ? validateConfigs(r.configs()) : Map.of();
        TopicDescription current = describe(name);

        if (r.partitions() != null) {
            int actual = current.partitions().size();
            if (r.partitions() <= actual) {
                throw new InvalidTopicRequestException(
                        "Kafka solo permite aumentar particiones (actual: " + actual + ", pedido: " + r.partitions() + ")");
            }
            KafkaCalls.await(
                    admin.createPartitions(Map.of(name, NewPartitions.increaseTo(r.partitions()))).all(),
                    timeout, "Aumentar particiones de " + name);
        }
        if (hasConfigs) {
            ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, name);
            Collection<AlterConfigOp> ops = new ArrayList<>();
            configs.forEach((k, v) -> ops.add(new AlterConfigOp(new ConfigEntry(k, v), AlterConfigOp.OpType.SET)));
            KafkaCalls.await(admin.incrementalAlterConfigs(Map.of(resource, ops)).all(), timeout,
                    "Actualizar configuracion de " + name);
        }
        return get(name);
    }

    public void delete(String name) {
        KafkaCalls.await(admin.deleteTopics(List.of(name)).all(), timeout, "Eliminar topico " + name);
    }

    public Map<String, String> getConfig(String name) {
        ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, name);
        var all = KafkaCalls.await(admin.describeConfigs(List.of(resource)).all(), timeout,
                "Leer configuracion de " + name);
        Map<String, String> result = new LinkedHashMap<>();
        for (ConfigEntry e : all.get(resource).entries()) {
            result.put(e.name(), e.value());
        }
        return result;
    }

    // ---- auxiliares ----

    private TopicDescription describe(String name) {
        Map<String, TopicDescription> map = KafkaCalls.await(
                admin.describeTopics(List.of(name)).allTopicNames(), timeout, "Describir topico " + name);
        return map.get(name);
    }

    private Map<String, String> validateConfigs(Map<String, String> configs) {
        if (configs == null) {
            return Map.of();
        }
        for (String key : configs.keySet()) {
            if (!ALLOWED_CONFIGS.contains(key)) {
                throw new InvalidTopicRequestException(
                        "Configuracion no permitida: '" + key + "'. Permitidas: " + ALLOWED_CONFIGS);
            }
        }
        return configs;
    }

    private int replication(TopicDescription d) {
        return d.partitions().isEmpty() ? 0 : d.partitions().get(0).replicas().size();
    }

    private PartitionDetail toPartition(TopicPartitionInfo p) {
        return new PartitionDetail(p.partition(),
                p.leader() == null ? -1 : p.leader().id(),
                p.replicas().stream().map(Node::id).toList(),
                p.isr().stream().map(Node::id).toList());
    }
}
