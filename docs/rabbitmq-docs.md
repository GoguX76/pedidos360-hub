# RabbitMQ — Mensajería

## Versión

RabbitMQ 4.x con Spring AMQP.

## Topología

### Exchanges

| Exchange | Tipo | Uso |
|---|---|---|
| `pedidos360.events` | topic | Eventos de pedidos (creado, estado cambiado) |
| `pedidos360.inventory` | direct | Eventos de inventario (stock bajo) |
| `pedidos360.dlx` | direct | Dead Letter Exchange para mensajes rechazados |

### Colas

| Cola | DLQ | Uso |
|---|---|---|
| `q.order-created` | `q.order-created.dlq` | Pedido creado → descontar stock |
| `q.order-status-changed` | `q.order-status-changed.dlq` | Cambio de estado |
| `q.stock-low` | `q.stock-low.dlq` | Stock bajo (≤10 unidades) |

## Flujo de mensajería

```
OrderCreatedPublisher (orders)
    ↓ exchange: pedidos360.events, routing: order.created
    ↓
q.order-created → OrderCreatedConsumer (products)
    ↓ valida evento, descontar stock
    ↓
StockLowPublisher (products) → q.stock-low
```

## Características

- **ACK manual**: `channel.basicAck()` / `channel.basicReject()`, con `spring.rabbitmq.listener.simple.acknowledge-mode=manual`
- **Reintentos**: hasta `app.messaging.retry.max-attempts` (3) reintentos, con el contador en el header `x-retry-count`
- **DLQ**: al agotar los reintentos, `basicReject` sin requeue deriva el mensaje a la cola de dead letters
- **Validación**: eventos validados con `Validator` antes de publicar
- **JSON**: `JacksonJsonMessageConverter` con type precedence
- **Cluster**: 2 nodos (`rabbit1`, `rabbit2`) en Docker Compose

## Configuración

`RabbitTopologyConfig` (módulo compartido `common-messaging`) declara exchanges, colas y bindings como beans de Spring.
Los nombres están centralizados en `MessagingTopology` y cada microservicio habilita la configuración con `@Import(RabbitTopologyConfig.class)`, de modo que la topología se declara **una sola vez** y no hay copias que puedan divergir.

## Consumidores

- `OrderCreatedConsumer` (products): valida el evento, descuenta stock, ACK explícito
- Si el procesamiento falla y el error es recuperable, el mensaje se reencola con `x-retry-count` incrementado y se reintenta
- Si el error es no recuperable (DTO inválido, producto inexistente, stock insuficiente) o se agotan los reintentos, `basicReject` sin requeue → mensaje va a DLQ
