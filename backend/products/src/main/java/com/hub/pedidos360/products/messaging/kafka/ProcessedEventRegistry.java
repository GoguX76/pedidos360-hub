package com.hub.pedidos360.products.messaging.kafka;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registro en memoria y acotado de eventos ya procesados, para no repetir
 * notificaciones cuando Kafka reentrega el mismo mensaje.
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
