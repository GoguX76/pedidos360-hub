package com.hub.pedidos360.kafka_admin.dto;

import java.util.Map;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTopicRequest(
        @NotBlank @Size(max = 249) @Pattern(regexp = "[a-zA-Z0-9._-]+", message = "solo letras, numeros, '.', '_' y '-'") String name,
        @Min(1) int partitions,
        @Min(1) short replicationFactor,
        Map<String, String> configs) {
}
