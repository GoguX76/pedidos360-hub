package com.hub.pedidos360.kafka_admin.dto;

/** Offset confirmado por el grupo, ultimo offset del topico y retraso (lag). */
public record GroupOffsetInfo(String topic, int partition, long committedOffset, long endOffset, long lag) {
}
