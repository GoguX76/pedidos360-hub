# Pedidos 360 Hub

Plataforma de gestión de pedidos para negocios. Permite administrar un catálogo de productos, gestionar el ciclo de vida de los pedidos y mantener un registro de despachos, todo en un solo lugar.

## Objetivo

Centralizar la operación de pedidos: desde que un cliente hace un pedido hasta que se despacha o entrega, pasando por la gestión de stock y la comunicación entre los distintos sistemas del negocio.

## Roles y permisos

La autenticación es con **Microsoft Entra ID** (JWT). El *App Role* `admin` viaja en el claim `roles` y el backend lo mapea a `ROLE_ADMIN` (ver `docs/entraid-docs.md`).

| Rol | Puede hacer |
|---|---|
| **Cliente** (`ROLE_CLIENTE`) | Ver el catálogo y crear/gestionar sus propios pedidos. |
| **Administrador** (`ROLE_ADMIN`) | Todo lo del cliente y, además, crear, actualizar y eliminar productos. |

El menú **Admin** (crear producto) solo aparece cuando el usuario tiene el rol `admin`. La página `/menu` es **pública**: se puede ver el catálogo sin iniciar sesión.

## Tecnologías

| Capa | Tecnología |
|---|---|
| Frontend | Angular + Tailwind CSS |
| Backend | Spring Boot (Java) |
| Mensajería | RabbitMQ |
| Base de datos | PostgreSQL (producción) / H2 (desarrollo) |
| Autenticación | Microsoft Entra ID (JWT) |
| Infraestructura | AWS (EC2, API Gateway) |

## Estructura del proyecto

```
pedidos360-hub/
├── frontend/          # App web (Angular)
├── backend/
│   ├── orders/        # Microservicio de pedidos
│   ├── products/      # Microservicio de catálogo
│   └── rabbitmq-admin/ # Administrador de RabbitMQ
├── infra/             # Configuración de infraestructura
└── docs/              # Documentación técnica
```

## Cómo ejecutar

**Requisitos:** Java 21+, Node.js 20+ y Docker.

**Frontend** (Angular):

```bash
cd frontend
npm install
npm start        # ng serve con proxy /api (ver proxy.conf.json)
```

**Backend** (un módulo por microservicio; por defecto usan el perfil `dev` con H2):

```bash
cd backend/orders        # o backend/products, backend/rabbitmq-admin
./gradlew bootRun        # en Windows: gradlew.bat bootRun
```

**Infraestructura RabbitMQ** (clúster de 2 nodos + administración):

```bash
cd infra/rabbitmq
cp .env.example .env     # ajusta las credenciales
docker compose up -d
```

## Cumplimiento de requisitos

* **Dos nodos RabbitMQ en clúster** (`rabbit1`, `rabbit2`) en `infra/rabbitmq/docker-compose.yml`.
* **Al menos 2 colas con su DLQ**: `q.order-created`, `q.order-status-changed` y `q.stock-low`, cada una con su `.dlq` vía el exchange `pedidos360.dlx`.
* **Exchanges `direct` y `topic`**: `pedidos360.inventory` / `pedidos360.dlx` (direct) y `pedidos360.events` (topic).
* **Microservicio administrador** (`rabbitmq-admin`) con creación y eliminación de colas, exchanges y bindings en `/api/v1/admin`.
* **ACK manual + reintentos + DLQ** en los consumidores (`acknowledge-mode=manual` y `MessageRetryHandler`).
* **`docker compose`** levanta el broker y los nodos en la nube.

## Documentación técnica

Índice completo en [`docs/README.md`](docs/README.md):

* [`docs/angular-docs.md`](docs/angular-docs.md) — frontend (Angular, Tailwind, MSAL, signals)
* [`docs/spring-boot-docs.md`](docs/spring-boot-docs.md) — backend (Spring Boot, JPA, Security, DTOs)
* [`docs/rabbitmq-docs.md`](docs/rabbitmq-docs.md) — mensajería (RabbitMQ, DLQ, clúster)
* [`docs/postgresql-docs.md`](docs/postgresql-docs.md) — base de datos (perfiles dev/prod)
* [`docs/aws-docs.md`](docs/aws-docs.md) — infraestructura (EC2, API Gateway)
* [`docs/entraid-docs.md`](docs/entraid-docs.md) — autenticación (Entra ID, JWT)
* [`docs/docker-docs.md`](docs/docker-docs.md) — contenedores (Dockerfiles, compose)
