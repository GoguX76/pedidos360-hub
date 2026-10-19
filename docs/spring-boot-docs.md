# Spring Boot — Backend

## Versión

Spring Boot 4.1.0 con Java 25.

## Microservicios

| Microservicio | Puerto | Responsabilidad |
|---|---|---|
| `orders` | 8081 | CRUD de pedidos, máquina de estados, cálculo de totales |
| `products` | 8082 | CRUD de productos, descuento de stock atómico |
| `rabbitmq-admin` | 8083 | Administración de RabbitMQ (colas, exchanges, bindings) |

## Estructura por microservicio

```
src/main/java/com/hub/pedidos360/<servicio>/
├── config/           # SecurityConfig, JwtDecoderConfig, RabbitMQConfig
├── controller/       # REST controllers
├── service/          # Lógica de negocio
├── repository/       # JPA repositories
├── model/            # Entidades JPA
├── dto/              # Request/Response DTOs
├── messaging/        # Publishers y consumidores RabbitMQ
└── exception/        # GlobalExceptionHandler
```

## Características principales

- **JPA/Hibernate**: persistencia con `ddl-auto=update`
- **DTOs**: separación entre entidad y API (request/response)
- **Validación**: Jakarta Validation (`@NotBlank`, `@NotNull`, etc.)
- **Manejo de errores**: `GlobalExceptionHandler` con respuestas consistentes
- **BigDecimal**: cálculos monetarios sin pérdida de precisión
- **@Transactional**: operaciones atómicas en create/update

## Seguridad

- **OAuth2 Resource Server**: valida JWT en cada request
- **JwtDecoder propio**: valida tokens v1 de Entra ID (issuer `sts.windows.net` + audience `api://...`)
- **STATELESS**: sin sesión de servidor
- **CORS**: configurado por propiedad `app.cors.allowed-origins`

## Tests

- Unitarios: Mockito para servicios
- Integración: `@SpringBootTest` + `MockMvc` para controladores
