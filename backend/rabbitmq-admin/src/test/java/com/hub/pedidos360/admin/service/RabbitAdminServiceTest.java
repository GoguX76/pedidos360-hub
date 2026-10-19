package com.hub.pedidos360.admin.service;

import com.hub.pedidos360.admin.dto.AdminActionResponse;
import com.hub.pedidos360.admin.dto.BindingRequest;
import com.hub.pedidos360.admin.dto.ExchangeRequest;
import com.hub.pedidos360.admin.dto.QueueRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitAdminServiceTest {

    @Mock
    private RabbitAdmin rabbitAdmin;

    @InjectMocks
    private RabbitAdminService rabbitAdminService;

    @Test
    void createQueue_declaraColaConDeadLetter() {
        QueueRequest request = new QueueRequest();
        request.setName("q.nueva-cola");
        request.setDurable(true);
        request.setDeadLetterExchange("pedidos360.dlx");
        request.setDeadLetterRoutingKey("q.nueva-cola.dlq");

        AdminActionResponse response = rabbitAdminService.createQueue(request);

        ArgumentCaptor<Queue> captor = ArgumentCaptor.forClass(Queue.class);
        verify(rabbitAdmin).declareQueue(captor.capture());

        Queue declared = captor.getValue();
        assertThat(declared.getName()).isEqualTo("q.nueva-cola");
        assertThat(declared.isDurable()).isTrue();
        assertThat(declared.getArguments())
                .containsEntry("x-dead-letter-exchange", "pedidos360.dlx")
                .containsEntry("x-dead-letter-routing-key", "q.nueva-cola.dlq");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getResourceType()).isEqualTo("QUEUE");
        assertThat(response.getResourceName()).isEqualTo("q.nueva-cola");
    }

    @Test
    void deleteQueue_llamaDeleteQueue() {
        AdminActionResponse response = rabbitAdminService.deleteQueue("q.nueva-cola");

        verify(rabbitAdmin).deleteQueue("q.nueva-cola");
        assertThat(response.isSuccess()).isTrue();
    }

    @Test
    void purgeQueue_llamaPurgeQueue() {
        rabbitAdminService.purgeQueue("q.nueva-cola");

        verify(rabbitAdmin).purgeQueue("q.nueva-cola", false);
    }

    @Test
    void createExchange_tipoTopic_creaTopicExchange() {
        ExchangeRequest request = new ExchangeRequest();
        request.setName("pedidos360.notificaciones");
        request.setType("topic");
        request.setDurable(true);

        rabbitAdminService.createExchange(request);

        ArgumentCaptor<org.springframework.amqp.core.Exchange> captor =
                ArgumentCaptor.forClass(org.springframework.amqp.core.Exchange.class);
        verify(rabbitAdmin).declareExchange(captor.capture());

        assertThat(captor.getValue()).isInstanceOf(TopicExchange.class);
        assertThat(captor.getValue().getName()).isEqualTo("pedidos360.notificaciones");
    }

    @Test
    void createExchange_tipoDirect_creaDirectExchange() {
        ExchangeRequest request = new ExchangeRequest();
        request.setName("pedidos360.direct");
        request.setType("direct");

        rabbitAdminService.createExchange(request);

        verify(rabbitAdmin).declareExchange(any(DirectExchange.class));
    }

    @Test
    void createExchange_tipoInvalido_lanzaExcepcion() {
        ExchangeRequest request = new ExchangeRequest();
        request.setName("pedidos360.weird");
        request.setType("inventado");

        assertThatThrownBy(() -> rabbitAdminService.createExchange(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tipo de exchange no soportado");
    }

    @Test
    void deleteExchange_llamaDeleteExchange() {
        rabbitAdminService.deleteExchange("pedidos360.notificaciones");

        verify(rabbitAdmin).deleteExchange("pedidos360.notificaciones");
    }

    @Test
    void createBinding_declaraBindingConRoutingKey() {
        BindingRequest request = new BindingRequest("q.nueva-cola", "pedidos360.notificaciones", "notif.*");

        rabbitAdminService.createBinding(request);

        ArgumentCaptor<Binding> captor = ArgumentCaptor.forClass(Binding.class);
        verify(rabbitAdmin).declareBinding(captor.capture());

        Binding binding = captor.getValue();
        assertThat(binding.getDestination()).isEqualTo("q.nueva-cola");
        assertThat(binding.getExchange()).isEqualTo("pedidos360.notificaciones");
        assertThat(binding.getRoutingKey()).isEqualTo("notif.*");
        assertThat(binding.getDestinationType()).isEqualTo(Binding.DestinationType.QUEUE);
    }

    @Test
    void deleteBinding_remueveBinding() {
        BindingRequest request = new BindingRequest("q.nueva-cola", "pedidos360.notificaciones", "notif.*");

        rabbitAdminService.deleteBinding(request);

        verify(rabbitAdmin).removeBinding(any(Binding.class));
    }
}
