package com.hub.pedidos360.kafka_admin.dto;

public record TopicInfo(String name, int partitions, int replicationFactor, boolean internal) {
}
