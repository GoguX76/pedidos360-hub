package com.hub.pedidos360.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminActionResponse {

    private boolean success;

    private String message;

    private String resourceType;

    private String resourceName;

    private Instant timestamp;
}
