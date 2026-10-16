package com.hub.pedidos360.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueueRequest {

    @NotBlank(message = "El nombre de la cola es obligatorio")
    private String name;

    private boolean durable = true;

    private boolean exclusive = false;

    private boolean autoDelete = false;

    private String deadLetterExchange;

    private String deadLetterRoutingKey;
}
