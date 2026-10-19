package com.hub.pedidos360.admin.controller;

import com.hub.pedidos360.admin.dto.AdminActionResponse;
import com.hub.pedidos360.admin.dto.BindingRequest;
import com.hub.pedidos360.admin.dto.ExchangeRequest;
import com.hub.pedidos360.admin.dto.QueueRequest;
import com.hub.pedidos360.admin.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class RabbitAdminController {

    private final RabbitAdminService rabbitAdminService;

    public RabbitAdminController(RabbitAdminService rabbitAdminService) {
        this.rabbitAdminService = rabbitAdminService;
    }

    @PostMapping("/queues")
    public ResponseEntity<AdminActionResponse> createQueue(@Valid @RequestBody QueueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rabbitAdminService.createQueue(request));
    }

    @DeleteMapping("/queues/{name}")
    public ResponseEntity<AdminActionResponse> deleteQueue(@PathVariable String name) {
        return ResponseEntity.ok(rabbitAdminService.deleteQueue(name));
    }

    @PostMapping("/queues/{name}/purge")
    public ResponseEntity<AdminActionResponse> purgeQueue(@PathVariable String name) {
        return ResponseEntity.ok(rabbitAdminService.purgeQueue(name));
    }

    @PostMapping("/exchanges")
    public ResponseEntity<AdminActionResponse> createExchange(@Valid @RequestBody ExchangeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rabbitAdminService.createExchange(request));
    }

    @DeleteMapping("/exchanges/{name}")
    public ResponseEntity<AdminActionResponse> deleteExchange(@PathVariable String name) {
        return ResponseEntity.ok(rabbitAdminService.deleteExchange(name));
    }

    @PostMapping("/bindings")
    public ResponseEntity<AdminActionResponse> createBinding(@Valid @RequestBody BindingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rabbitAdminService.createBinding(request));
    }

    @DeleteMapping("/bindings/{queueName}/{exchangeName}/{routingKey}")
    public ResponseEntity<AdminActionResponse> deleteBinding(
            @PathVariable String queueName,
            @PathVariable String exchangeName,
            @PathVariable String routingKey) {
        BindingRequest request = new BindingRequest(queueName, exchangeName, routingKey);
        return ResponseEntity.ok(rabbitAdminService.deleteBinding(request));
    }
}
