# Kafka y notificaciones

Integración de Apache Kafka para el flujo de notificaciones de Pedidos360.
RabbitMQ se mantiene como transporte principal de pedidos e inventario; Kafka
se agrega como canal de eventos para las notificaciones.

## Arquitectura del flujo

```text
                       topico: pedidos.eventos
  orders  ──────────────────────────────────────────────►  products
  (productor)                OrderEvent                     (consumidor)
      ▲                                                       grupo:
      │                                                       products-order-events
      │                                                       │
      │                                                       └─► notificacion en log
      │
      │            topico: inventario.alertas
      └──────────────────────────────────────────────────────  products
        (consumidor)          InventoryAlertEvent              (productor)
        grupo: orders-notifications
        │
        └─► notificacion en log
```

- **orders** publica `OrderEvent` cuando se crea un pedido o cambia de estado.
- **products** consume `OrderEvent` solo para notificar. El descuento de stock
  sigue ocurriendo por RabbitMQ, de modo que no se descuenta dos veces.
- **products** publica `InventoryAlertEvent` cuando el stock queda en o por
  debajo del umbral.
- **orders** consume `InventoryAlertEvent` para emitir la notificación de stock
  crítico.

Las notificaciones son simuladas: se escriben en el log con el prefijo
`[NOTIFICACION - PEDIDOS]` o `[NOTIFICACION - PRODUCTOS]`. Reemplazar el cuerpo
de `InventoryNotificationService` u `OrderNotificationService` por un proveedor
real no afecta al resto del flujo.

## Tópicos y grupos

| Tópico | Productor | Consumidor | Grupo | DLT |
|---|---|---|---|---|
| `pedidos.eventos` | orders | products | `products-order-events` | `pedidos.eventos.DLT` |
| `inventario.alertas` | products | orders | `orders-notifications` | `inventario.alertas.DLT` |

Los nombres viven en
`backend/common-messaging/src/main/java/com/hub/pedidos360/messaging/kafka/KafkaTopics.java`
y los tópicos se crean al arrancar `kafka-admin` según
`backend/kafka-admin/src/main/resources/application.yml`.

## Contratos de eventos

Ambos DTO son `record` inmutables, con validación Jakarta y fechas en texto
ISO-8601. No dependen de entidades JPA.

### `OrderEvent`

```text
eventId, eventType, orderId, customerId, customerName, branchId,
dispatchType, status, totalAmount, items[], occurredAt
```

`eventType` puede ser `ORDER_CREATED` o `ORDER_STATUS_CHANGED`.
Cada item incluye `productId`, `productName`, `quantity` y `unitPrice`.

### `InventoryAlertEvent`

```text
eventId, productId, productName, currentStock, threshold, alertAt
```

## Configuración

`orders` y `products` comparten el bootstrap de brokers:

```properties
spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS:localhost:9092,localhost:9093}
```

Grupos y reintentos:

```properties
# orders
app.kafka.orders-consumer-group=${KAFKA_ORDERS_GROUP:orders-notifications}

# products
app.kafka.products-consumer-group=${KAFKA_PRODUCTS_GROUP:products-order-events}

app.kafka.retry.max-attempts=${KAFKA_RETRY_MAX_ATTEMPTS:2}
```

La configuración programática está en
`orders/src/main/java/.../config/KafkaConfig.java` y
`products/src/main/java/.../config/KafkaConfig.java`. Allí se definen:

- serialización JSON con `JacksonJsonSerializer` / `JacksonJsonDeserializer`;
- `KafkaTemplate` con `acks=all` e idempotencia;
- `ConsumerFactory` con `ErrorHandlingDeserializer`;
- `DefaultErrorHandler` con reintentos y derivación a `<topico>.DLT`.

## Manejo de errores

1. Un evento inválido (`IllegalArgumentException` de validación) no se
   reintenta: se publica en el tópico `.DLT`.
2. Un error recuperable se reintenta `app.kafka.retry.max-attempts` veces.
3. Agotados los intentos, el mensaje se publica en `<topico>.DLT` sin perder el
   resto de la partición.
4. Los consumidores registran el `eventId` procesado en un registro acotado en
   memoria para no repetir notificaciones ante reentregas. En producción debe
   reemplazarse por una tabla o Redis.

## Seguridad en kafka-admin

- `GET /api/kafka/**`: requiere token válido.
- `POST`, `PUT` y `DELETE /api/kafka/**`: requieren `ROLE_ADMIN`.
- `kafka-admin.bootstrap-fail-fast=true`: si no puede crear los tópicos, el
  servicio no arranca y el fallo no pasa desapercibido.

## Cómo levantar y demostrar

1. Levantar Kafka y Kafka UI:

   ```bash
   cd infra/kafka
   docker compose up -d
   ```

   Kafka UI queda en `http://localhost:8090`.

2. Arrancar `kafka-admin` (crea los tópicos y los DLT):

   ```bash
   cd backend/kafka-admin
   ./gradlew bootRun
   ```

3. Arrancar `orders` y `products`:

   ```bash
   cd backend/orders && ./gradlew bootRun
   cd backend/products && ./gradlew bootRun
   ```

4. Crear un pedido (con un token válido) y revisar:

   - log de `orders`: evento `ORDER_CREATED` publicado;
   - log de `products`: `[NOTIFICACION - PRODUCTOS] ...`;
   - Kafka UI: mensaje en `pedidos.eventos`.

5. Provocar stock bajo (crear o actualizar un producto con stock ≤ 10) y
   revisar:

   - log de `products`: alerta publicada;
   - log de `orders`: `[NOTIFICACION - PEDIDOS] Stock critico ...`;
   - Kafka UI: mensaje en `inventario.alertas`.

6. Para probar la DLT, publicar un JSON inválido en cualquiera de los tópicos
   desde Kafka UI y verificar que aparece en `<topico>.DLT`.

## Referencias

- Infraestructura: [`infra/kafka/docker-compose.yml`](../infra/kafka/docker-compose.yml)
- Contratos: [`backend/common-messaging/src/main/java/com/hub/pedidos360/messaging/kafka/`](../backend/common-messaging/src/main/java/com/hub/pedidos360/messaging/kafka/)
- Administrador: [`backend/kafka-admin/`](../backend/kafka-admin/)
