# Arquitectura — Pedidos 360 Hub

## Diagrama lógico

```
┌────────────┐      ┌──────────────┐      ┌─────────────────┐
│  Frontend  │─────▶│ API Gateway  │─────▶│ orders   (:8081)│
│ Angular    │      │ AWS          │      ├─────────────────┤
│ (:4200)    │      │ us-east-1    │─────▶│ products (:8082)│
└────────────┘      └──────────────┘      └─────────────────┘
       │                                          ▲
       │ login (MSAL)                             │ JWT Entra ID
       ▼                                          │
┌────────────┐                          valida `iss` + `aud`
│ Microsoft  │
│ Entra ID   │
└────────────┘
```

## Componentes

### `backend/orders` (puerto 8081)

Microservicio de pedidos. Crea pedidos calculando el total
(`unitPrice × quantity` por ítem, en `BigDecimal`) y gestiona su ciclo de vida
con una máquina de estados estricta (ver [`api.md`](api.md)).

Paquetes: `controller`, `service`, `repository` (JPA), `model`
(`Order`, `OrderItem`, `OrderStatus`, `DispatchType`), `dto`, `exception`, `config`.

### `backend/products` (puerto 8082)

Microservicio de catálogo: CRUD de productos, consulta de disponibles y
descuento de stock atómico (`POST /{id}/decrement`).

### `frontend` (puerto 4200)

Angular 22 con `@azure/msal-angular`. Rutas:

| Ruta | Acceso |
|---|---|
| `/login` | Pública |
| `/home` | Protegida con `MsalGuard` |
| `/` | Redirige a `/login` |

`ApiService` (`core/api`) consume el gateway; el `MsalInterceptor` adjunta
el JWT automáticamente a las llamadas bajo `/api/v1/orders*`.

## Datos

| Perfil | Motor | Config |
|---|---|---|
| `dev` (por defecto) | H2 en memoria | `application-dev.properties` (`jdbc:h2:mem:…`, consola en `/h2-console`) |
| `prod` | PostgreSQL | `application-prod.properties` (vía `DB_URL`, `DB_USER`, `DB_PASSWORD`) |

Cada microservicio tiene su propia base (`ddl-auto=update`).

## Despliegue

* Cada backend tiene `Dockerfile` multi-etapa (build con JDK 25 + `bootJar`,
  runtime JRE 25) y `HEALTHCHECK` contra `/actuator/health` en su puerto.
* Actuator expone `health` e `info` sin autenticación; todo lo demás exige JWT.
* CORS se configura por propiedad (`app.cors.allowed-origins`, por defecto
  `http://localhost:4200`).
