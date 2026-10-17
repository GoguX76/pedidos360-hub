# Pendientes EVA2 - Pedidos 360 Hub

> **Nota:** La mayoría de los requisitos EVA2 ya están implementados.
> Este documento lo lee un modelo de IA para entender qué falta.

---

## Estado General

| Requisito EVA2 | Estado |
|---|---|
| Productores y consumidores RabbitMQ | **OK** |
| DLQ (Dead Letter Queues) + logs | **OK** |
| Microservicio administrador RabbitMQ | **OK** (rabbitmq-admin) |
| Cluster 2 nodos RabbitMQ | **OK** |
| Al menos 2 colas con DLQ | **OK** (3 colas) |
| Exchanges direct y topic | **OK** |
| Docker Compose RabbitMQ | **OK** |
| Configuraciones centralizadas | **OK** |
| Una clase por caso de uso | **OK** |
| Clases aisladas del negocio | **OK** |
| Consumidor con clases separadas | **OK** |
| ACK explícito en consumidores | **OK** |
| Broker como servicio | **OK** |
| Validación con DTO | **OK** |

---

## Pendientes Reales

### 1. Integración del rabbitmq-admin con orders/products

**Situación:** El microservicio `rabbitmq-admin` existe y funciona, pero los
microservicios `orders` y `products` **aún declaran sus colas/exchanges/bindings
en código** (`RabbitMQConfig.java`).

**Opcional pero recomendado:** Modificar `orders` y `products` para que consuman
las colas/exchanges creados por el `rabbitmq-admin` en vez de declararlos
localmente. Esto haría que el administrador sea realmente el punto central.

**Alternativa:** Dejarlo está así. El requisito EVA2 pide "existencia de un
microservicio administrador", no que sea obligatorio usarlo.

### 2. Docker Compose completo (todos los servicios en un solo archivo)

**Situación:** Actualmente hay dos `docker-compose.yml`:
- `infra/rabbitmq/docker-compose.yml` → solo RabbitMQ
- Cada microservicio tiene su propio `Dockerfile`

**Pendiente (opcional):** Crear un `docker-compose.yml` en la raíz que levante
todos los servicios juntos (orders, products, rabbitmq-admin, rabbitmq, postgres)
para facilitar el despliegue en EC2.

### 3. Verificación manual en EC2

**S:** Los `Dockerfile` y `deploy-ec2.sh` están creados pero no se han probado
en una EC2 real.

**Pasos para probar:**
1. `cd backend/rabbitmq-admin && ./deploy-ec2.sh`
2. Verificar que el contenedor corre: `docker ps`
3. Probar healthcheck: `curl http://localhost:8083/actuator/health`
4. Probar un endpoint con token JWT

### 4. Actualizar documentación

**Pendiente:** Agregar documentación del `rabbitmq-admin` en:
- `README.md` (tabla de componentes)
- `docs/api.md` (endpoints del administrador)
- `docs/arquitectura.md` (diagrama actualizado)

---

## Resumen

**El proyecto cumple todos los requisitos EVA2.** Los pendientes son mejoras
de integración y documentación, no requisitos faltantes.

**Prioridad de pendientes:**

| Prioridad | Pendiente |
|---|---|
| ALTA | Probar deploy en EC2 |
| MEDIA | Actualizar documentación (README, api.md, arquitectura.md) |
| BAJA | Integración orders/products con rabbitmq-admin |
| BAJA | Docker Compose unificado |
