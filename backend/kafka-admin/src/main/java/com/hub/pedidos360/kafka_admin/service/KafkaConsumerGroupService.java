package com.hub.pedidos360.kafka_admin.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.stereotype.Service;

import com.hub.pedidos360.kafka_admin.config.KafkaAdminProperties;
import com.hub.pedidos360.kafka_admin.dto.ConsumerGroupDetail;
import com.hub.pedidos360.kafka_admin.dto.ConsumerGroupInfo;
import com.hub.pedidos360.kafka_admin.dto.GroupOffsetInfo;
import com.hub.pedidos360.kafka_admin.dto.MemberInfo;
import com.hub.pedidos360.kafka_admin.exception.TopicNotFoundException;

@Service
public class KafkaConsumerGroupService {

    private final Admin admin;
    private final int timeout;

    public KafkaConsumerGroupService(Admin admin, KafkaAdminProperties props) {
        this.admin = admin;
        this.timeout = props.timeoutSeconds();
    }

    public List<ConsumerGroupInfo> list() {
        return KafkaCalls.await(admin.listConsumerGroups().all(), timeout, "Listar consumer groups").stream()
                .map(g -> new ConsumerGroupInfo(g.groupId(), g.state().map(Object::toString).orElse("UNKNOWN")))
                .sorted((a, b) -> a.groupId().compareTo(b.groupId()))
                .toList();
    }

    public ConsumerGroupDetail describe(String groupId) {
        Map<String, ConsumerGroupDescription> map = KafkaCalls.await(
                admin.describeConsumerGroups(List.of(groupId)).all(), timeout, "Describir grupo " + groupId);
        ConsumerGroupDescription d = map.get(groupId);
        if (d == null || "DEAD".equalsIgnoreCase(String.valueOf(d.state()))) {
            throw new TopicNotFoundException("El consumer group '" + groupId + "' no existe");
        }
        String state = String.valueOf(d.state());
        List<MemberInfo> members = d.members().stream().map(this::toMember).toList();
        Integer coordinator = d.coordinator() == null ? null : d.coordinator().id();
        return new ConsumerGroupDetail(groupId, state, d.partitionAssignor(), coordinator, members);
    }

    /** Offsets confirmados por el grupo y su retraso (lag) frente al final de cada particion. */
    public List<GroupOffsetInfo> offsets(String groupId) {
        Map<TopicPartition, OffsetAndMetadata> committed = KafkaCalls.await(
                admin.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata(), timeout,
                "Leer offsets del grupo " + groupId);
        if (committed.isEmpty()) {
            return List.of();
        }
        Map<TopicPartition, OffsetSpec> request = new HashMap<>();
        committed.keySet().forEach(tp -> request.put(tp, OffsetSpec.latest()));
        Map<TopicPartition, ListOffsetsResultInfo> ends = KafkaCalls.await(
                admin.listOffsets(request).all(), timeout, "Leer offsets finales");

        return committed.entrySet().stream()
                .map(e -> {
                    long end = ends.get(e.getKey()).offset();
                    long done = e.getValue().offset();
                    return new GroupOffsetInfo(e.getKey().topic(), e.getKey().partition(), done, end,
                            Math.max(0, end - done));
                })
                .sorted((a, b) -> a.topic().equals(b.topic())
                        ? Integer.compare(a.partition(), b.partition())
                        : a.topic().compareTo(b.topic()))
                .toList();
    }

    private MemberInfo toMember(MemberDescription m) {
        List<String> partitions = m.assignment().topicPartitions().stream()
                .map(tp -> tp.topic() + "-" + tp.partition()).sorted().toList();
        return new MemberInfo(m.consumerId(), m.clientId(), m.host(), partitions);
    }
}
