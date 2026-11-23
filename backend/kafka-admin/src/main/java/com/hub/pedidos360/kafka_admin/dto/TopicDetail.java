package com.hub.pedidos360.kafka_admin.dto;

import java.util.List;
import java.util.Map;

public record TopicDetail(String name, boolean internal, List<PartitionDetail> partitions, Map<String, String> configs) {
}
