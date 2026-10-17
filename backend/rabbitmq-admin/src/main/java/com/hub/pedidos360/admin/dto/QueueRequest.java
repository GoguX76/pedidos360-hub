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

    private Boolean durable = true;

    private Boolean exclusive = false;

    private Boolean autoDelete = false;

    private String deadLetterExchange;

    private String deadLetterRoutingKey;
}
