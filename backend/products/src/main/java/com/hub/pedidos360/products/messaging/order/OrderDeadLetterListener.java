package com.hub.pedidos360.products.messaging.order;

import com.hub.pedidos360.messaging.DeadLetterLogger;
import com.hub.pedidos360.messaging.MessagingTopology;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Observabilidad de las colas muertas del dominio de pedidos: registra el
 * motivo (header {@code x-death}) y el payload de los mensajes descartados.
 */
@Component
public class OrderDeadLetterListener {

    private static final Logger log = LoggerFactory.getLogger(OrderDeadLetterListener.class);

    @RabbitListener(queues = {
            MessagingTopology.ORDER_CREATED_DLQ,
            MessagingTopology.ORDER_STATUS_CHANGED_DLQ
    })
    public void onDeadLetter(Message message, Channel channel) throws IOException {
        String queue = message.getMessageProperties().getConsumerQueue();
        DeadLetterLogger.log(log, queue, message);
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
