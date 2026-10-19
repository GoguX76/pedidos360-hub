# Pedidos 360 Hub

Plataforma de gestión de pedidos para negocios. Permite administrar un catálogo de productos, gestionar el ciclo de vida de los pedidos y mantener un registro de despachos, todo en un solo lugar.

## Objetivo

Centralizar la operación de pedidos: desde que un cliente hace un pedido hasta que se despacha o entrega, pasando por la gestión de stock y la comunicación entre los distintos sistemas del negocio.

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

## Documentación técnica

* [`docs/arquitectura.md`](docs/arquitectura.md) — arquitectura y componentes
* [`docs/api.md`](docs/api.md) — endpoints de la API
* [`docs/seguridad-jwt.md`](docs/seguridad-jwt.md) — autenticación
* [`docs/desarrollo.md`](docs/desarrollo.md) — cómo correr y probar
