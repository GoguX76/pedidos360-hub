package com.hub.pedidos360.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRequest {

    @NotBlank(message = "El nombre del exchange es obligatorio")
    private String name;

    @NotBlank(message = "El tipo de exchange es obligatorio")
    private String type;

    private boolean durable = true;

    private boolean autoDelete = false;
}
