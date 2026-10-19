package com.hub.pedidos360.messaging;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Ejecuta una accion recien cuando la transaccion actual hizo commit, evitando
 * publicar eventos de cambios que la base de datos pueda revertir.
 * Si no hay transaccion activa, la accion se ejecuta de inmediato.
 */
public final class AfterCommitExecutor {

    private AfterCommitExecutor() {
    }

    public static void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
