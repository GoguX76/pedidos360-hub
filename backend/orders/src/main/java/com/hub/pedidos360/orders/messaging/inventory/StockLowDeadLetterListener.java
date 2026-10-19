package com.hub.pedidos360.orders.messaging.inventory;

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
 * Observabilidad de la cola muerta de inventario: registra el motivo
 * (header {@code x-death}) y el payload del mensaje que no pudo procesarse.
 */
@Component
public class StockLowDeadLetterListener {

    private static final Logger log = LoggerFactory.getLogger(StockLowDeadLetterListener.class);

    @RabbitListener(queues = MessagingTopology.STOCK_LOW_DLQ)
    public void onDeadLetter(Message message, Channel channel) throws IOException {
        DeadLetterLogger.log(log, MessagingTopology.STOCK_LOW_DLQ, message);
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
