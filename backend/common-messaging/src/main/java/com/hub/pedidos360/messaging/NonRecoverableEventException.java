package com.hub.pedidos360.messaging;

/**
 * Marca un error como NO recuperable: reintentar el mismo mensaje no cambiaria
 * el resultado (por ejemplo un payload invalido), por lo que se envia
 * directamente a la cola muerta sin consumir reintentos.
 */
public class NonRecoverableEventException extends RuntimeException {

    public NonRecoverableEventException(String message) {
        super(message);
    }

    public NonRecoverableEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
