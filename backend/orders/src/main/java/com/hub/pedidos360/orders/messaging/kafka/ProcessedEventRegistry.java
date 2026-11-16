package com.hub.pedidos360.orders.messaging.kafka;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registro en memoria y acotado de eventos ya procesados.
 *
 * <p>Evita repetir una notificacion cuando Kafka reentrega el mismo evento
 * (por ejemplo, despues de un rebalanceo). Es volatil por diseno: en
 * produccion deberia reemplazarse por una tabla o Redis.</p>
 */
@Component
public class ProcessedEventRegistry {

    private static final int MAX_TRACKED_EVENTS = 10_000;

    private final Set<String> processed = Collections.newSetFromMap(
            Collections.synchronizedMap(new LinkedHashMap<>() {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    return size() > MAX_TRACKED_EVENTS;
                }
            }));

    public boolean isProcessed(String eventId) {
        return processed.contains(eventId);
    }

    public void markProcessed(String eventId) {
        processed.add(eventId);
    }
}
