package com.hub.pedidos360.kafka_admin.dto;

import java.util.Map;

import jakarta.validation.constraints.Min;

/** Ambos campos son opcionales: se puede subir particiones, cambiar configs o ambos. */
public record UpdateTopicRequest(@Min(1) Integer partitions, Map<String, String> configs) {
}
