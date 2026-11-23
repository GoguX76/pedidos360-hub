package com.hub.pedidos360.kafka_admin.service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.InvalidReplicationFactorException;
import org.apache.kafka.common.errors.InvalidRequestException;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;

import com.hub.pedidos360.kafka_admin.exception.InvalidTopicRequestException;
import com.hub.pedidos360.kafka_admin.exception.KafkaAdminException;
import com.hub.pedidos360.kafka_admin.exception.TopicAlreadyExistsException;
import com.hub.pedidos360.kafka_admin.exception.TopicNotFoundException;

/** Espera un resultado de Kafka y traduce sus errores a excepciones propias. */
final class KafkaCalls {

    private KafkaCalls() {
    }

    static <T> T await(KafkaFuture<T> future, int timeoutSeconds, String action) {
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaAdminException(action + ": operacion interrumpida", e);
        } catch (TimeoutException e) {
            throw new KafkaAdminException(action + ": Kafka no respondio en " + timeoutSeconds + " s", e);
        } catch (ExecutionException e) {
            throw translate(action, e.getCause() != null ? e.getCause() : e);
        }
    }

    private static KafkaAdminException translate(String action, Throwable cause) {
        if (cause instanceof UnknownTopicOrPartitionException) {
            return new TopicNotFoundException(action + ": el topico no existe");
        }
        if (cause instanceof TopicExistsException) {
            return new TopicAlreadyExistsException(action + ": el topico ya existe");
        }
        if (cause instanceof InvalidTopicException || cause instanceof InvalidConfigurationException
                || cause instanceof InvalidPartitionsException || cause instanceof InvalidReplicationFactorException
                || cause instanceof InvalidRequestException) {
            return new InvalidTopicRequestException(action + ": " + cause.getMessage());
        }
        return new KafkaAdminException(action + ": " + cause.getMessage(), cause);
    }
}
