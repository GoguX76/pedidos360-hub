package com.hub.pedidos360.kafka_admin.dto;

import java.util.List;

public record PartitionDetail(int partition, int leader, List<Integer> replicas, List<Integer> isr) {
}
