package com.hub.pedidos360.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class QueueRequest {

    @NotBlank(message = "El nombre de la cola es obligatorio")
    private String name;

    private Boolean durable;

    private Boolean exclusive;

    private Boolean autoDelete;

    private String deadLetterExchange;

    private String deadLetterRoutingKey;

    /** Valores por defecto cuando el cliente no envía estos campos. */
    public QueueRequest() {
        this.durable = true;
        this.exclusive = false;
        this.autoDelete = false;
    }
}
