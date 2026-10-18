package com.hub.pedidos360.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ExchangeRequest {

    @NotBlank(message = "El nombre del exchange es obligatorio")
    private String name;

    @NotBlank(message = "El tipo de exchange es obligatorio")
    private String type;

    private Boolean durable;

    private Boolean autoDelete;

    /** Valores por defecto cuando el cliente no envía estos campos. */
    public ExchangeRequest() {
        this.durable = true;
        this.autoDelete = false;
    }
}
