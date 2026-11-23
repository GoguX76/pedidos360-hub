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
- **order-streams** agrupa los eventos de `pedidos.eventos` por estado y publica
  el conteo acumulado de eventos en `pedidos.conteo-por-estado`. Cuenta eventos,
  no pedidos que actualmente estén en cada estado.

Las notificaciones son simuladas: se escriben en el log con el prefijo
`[NOTIFICACION - PEDIDOS]` o `[NOTIFICACION - PRODUCTOS]`. Reemplazar el cuerpo
de `InventoryNotificationService` u `OrderNotificationService` por un proveedor
real no afecta al resto del flujo.

## Tópicos y grupos

| Tópico | Productor | Consumidor | Grupo | DLT |
|---|---|---|---|---|
| `pedidos.eventos` | orders | products | `products-order-events` | `pedidos.eventos.DLT` |
| `inventario.alertas` | products | orders | `orders-notifications` | `inventario.alertas.DLT` |
| `pedidos.conteo-por-estado` | order-streams | Kafka UI | `pedidos-status-count` | — |

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
spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS:localhost:9092,localhost:9093,localhost:9094}
```

El Compose local levanta tres brokers (`kafka1`, `kafka2` y `kafka3`) en los
puertos externos `9092`, `9093` y `9094`. Cada tópico y su DLT se crean con
tres particiones y factor de réplica 3; `min.insync.replicas=2` permite seguir
escribiendo si un broker queda fuera, siempre que dos réplicas sigan sincronizadas.
Los tres brokers locales comparten la misma máquina, por lo que esto valida la
configuración de Kafka pero no protege contra la caída de esa máquina.

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
4. Cada consumidor guarda los `eventId` procesados en la tabla
   `processed_kafka_events` de la base de datos de su servicio. La clave
   primaria evita registrar dos veces el mismo evento. En el perfil de
   producción, los servicios usan PostgreSQL, por lo que estos registros
   sobreviven al reinicio de la aplicación mientras la base de datos conserve
   sus datos. En desarrollo, H2 está configurada en memoria y los registros se
   pierden al reiniciar.

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

   Kafka UI queda en `http://localhost:8090` y los tres brokers publican sus
   puertos locales `9092`, `9093` y `9094`.

2. Arrancar `kafka-admin` para crear los tópicos y sus DLT. En Windows,
   ejecuta desde la raíz del proyecto:

   ```bash
   cd backend\kafka-admin
   .\gradlew.bat bootRun
   ```

   El servicio no tiene UI propia: su API de administración escucha en `8084`.
   Los tópicos configurados se crean al iniciar si aún no existen. En Kafka UI
   actualiza la lista de tópicos para verlos.

3. Arrancar `orders` y `products`, cada uno en una terminal separada:

   ```bash
   cd backend\orders
   .\gradlew.bat bootRun
   ```

   ```bash
   cd backend\products
   .\gradlew.bat bootRun
   ```

   Los grupos `orders-notifications` y `products-order-events` aparecen cuando
   los listeners de esos servicios se conectan. Genera eventos para ver
   actividad y offsets: crea un pedido o provoca una alerta de stock bajo.

4. Después de que `kafka-admin` haya creado los tópicos, inicia Kafka Streams:

   ```bash
   cd infra\kafka
   docker compose --profile streams up -d --build order-streams
   ```

   El servicio cuenta eventos por estado y publica cada actualización en
   `pedidos.conteo-por-estado` (clave: estado; valor: conteo). Es un conteo de
   eventos, no del número actual de pedidos en cada estado. Kafka UI muestra
   `pedidos-status-count` como consumer group.

5. Crear un pedido (con un token válido) y revisar:

   - log de `orders`: evento `ORDER_CREATED` publicado;
   - log de `products`: `[NOTIFICACION - PRODUCTOS] ...`;
   - Kafka UI: mensaje en `pedidos.eventos`.

6. Provocar stock bajo (crear o actualizar un producto con stock ≤ 10) y
   revisar:

   - log de `products`: alerta publicada;
   - log de `orders`: `[NOTIFICACION - PEDIDOS] Stock critico ...`;
   - Kafka UI: mensaje en `inventario.alertas`.

7. Para probar la DLT, publicar un JSON inválido en cualquiera de los tópicos
   desde Kafka UI y verificar que aparece en `<topico>.DLT`.

## Acceso remoto a Kafka UI en AWS

Por defecto, Kafka UI solo publica `127.0.0.1:8090` en la instancia. Para
acceder directamente por HTTPS, el Compose incluye un proxy Nginx opcional,
protegido con autenticación básica. No abre HTTP ni cambia la exposición local
de Kafka UI.

1. Copia a `/home/ec2-user/kafka` en la EC2 el Compose actualizado, el archivo
   `kafka-ui/nginx.conf` y `setup-kafka-ui-access.sh`.
2. En AWS Console, abre **EC2 → Instances → tu instancia Kafka** y copia el
   valor **Public IPv4 address** (la IP de la instancia, no la de tu PC).
   En la EC2, genera los archivos privados de autenticación y el certificado
   para esa IP:

   ```bash
   cd /home/ec2-user/kafka
   chmod +x setup-kafka-ui-access.sh
   ./setup-kafka-ui-access.sh <IPv4-publica-de-la-EC2>
   ```

   El script solicita usuario y contraseña; exige una contraseña de al menos
   16 caracteres. La clave TLS y el archivo de autenticación se guardan bajo
   `infra/kafka/secrets/`, excluido de Git.
3. En el Security Group de la instancia, agrega inbound TCP `443` y selecciona
   **My IP** como origen. AWS detecta la IP pública desde la que estás usando
   la consola y la agrega como `/32`; esa es la IP de tu PC/red, distinta de la
   IPv4 pública de la instancia usada en el paso anterior. No abras `8090` ni
   los puertos de los brokers a Internet.
4. Activa el proxy:

   ```bash
   docker compose --profile public-ui up -d kafka-ui-public
   ```

   Abre `https://<IPv4-publica-de-la-EC2>` e ingresa las credenciales creadas.
   El certificado es autofirmado: el navegador advertirá que no confía en él.
   Esto cifra la conexión, pero no ofrece la misma verificación de identidad
   que un certificado emitido para un dominio de tu propiedad. Para acceso
   habitual, es preferible usar un dominio y un certificado de una CA confiable.

Para cerrar el acceso remoto sin apagar Kafka UI local:

```bash
docker compose --profile public-ui stop kafka-ui-public
```

La aplicación Kafka Streams se despliega como un contenedor adicional en la
misma red privada de Kafka. Antes de iniciarla en EC2, despliega la actualización
de `kafka-admin` para que cree `pedidos.conteo-por-estado`; no hace falta abrir
puertos adicionales en el Security Group.

## Referencias

- Infraestructura: [`infra/kafka/docker-compose.yml`](../infra/kafka/docker-compose.yml)
- Contratos: [`backend/common-messaging/src/main/java/com/hub/pedidos360/messaging/kafka/`](../backend/common-messaging/src/main/java/com/hub/pedidos360/messaging/kafka/)
- Administrador: [`backend/kafka-admin/`](../backend/kafka-admin/)
