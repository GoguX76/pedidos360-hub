# PostgreSQL — Base de datos

## Uso

PostgreSQL es la base de datos de producción. Cada microservicio tiene su propia base.

## Perfiles

| Perfil | Motor | Configuración |
|---|---|---|
| `dev` | H2 en memoria | `application-dev.properties` (`jdbc:h2:mem:pedidos360`) |
| `prod` | PostgreSQL | `application-prod.properties` (vía `DB_URL`, `DB_USER`, `DB_PASSWORD`) |

## Configuración de producción

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
```

## Bases de datos

| Microservicio | Base de datos | Tablas |
|---|---|---|
| `orders` | `pedidos360` | `orders`, `order_items` |
| `products` | `pedidos360` | `products` |
| `rabbitmq-admin` | — | No usa base de datos |

## Características

- `ddl-auto=update`: JPA crea/actualiza tablas automáticamente
- Consola H2 disponible en `/h2-console` (solo dev)
- Cada microservicio tiene su propia conexión independiente
