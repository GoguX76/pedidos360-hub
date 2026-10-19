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

- **ACK manual**: `channel.basicAck()` / `channel.basicReject()`
- **DLQ**: mensajes rechazados van a la cola de dead letters
- **Validación**: eventos validados con `Validator` antes de publicar
- **JSON**: `JacksonJsonMessageConverter` con type precedence
- **Cluster**: 2 nodos (`rabbit1`, `rabbit2`) en Docker Compose

## Configuración

`RabbitMQConfig.java` declara exchanges, colas y bindings como beans de Spring.
Cada microservicio tiene su propia copia de la configuración.

## Consumidores

- `OrderCreatedConsumer` (products): valida el evento, descuenta stock, ACK explícito
- Si el procesamiento falla: `basicReject` sin requeue → mensaje va a DLQ
