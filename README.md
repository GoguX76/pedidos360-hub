# Pedidos 360 Hub

Plataforma de gestión de pedidos: catálogo de productos, ciclo de vida de pedidos
y frontend Angular con login corporativo (Microsoft Entra ID).

## Componentes

| Componente | Tecnología | Puerto local | Descripción |
|---|---|---|---|
| `backend/orders` | Spring Boot + Gradle | `8081` | Microservicio de pedidos (`/api/v1/orders`) |
| `backend/products` | Spring Boot + Gradle | `8082` | Microservicio de productos (`/api/v1/products`) |
| `frontend` | Angular 22 + MSAL | `4200` | App web (`/login`, `/home` protegido) |
| API Gateway | AWS (`us-east-1`) | — | `https://y2fmjg2ed2.execute-api.us-east-1.amazonaws.com` |

## Inicio rápido

```powershell
# Backend (requiere JDK 25)
cd backend/orders; .\gradlew.bat bootRun     # :8081
cd backend/products; .\gradlew.bat bootRun   # :8082

# Frontend (requiere Node 20+)
cd frontend; npm install; npm start           # :4200
```

## Documentación

* [`docs/arquitectura.md`](docs/arquitectura.md) — componentes, puertos, despliegue.
* [`docs/api.md`](docs/api.md) — endpoints de orders y products.
* [`docs/seguridad-jwt.md`](docs/seguridad-jwt.md) — autenticación JWT con Entra ID.
* [`docs/desarrollo.md`](docs/desarrollo.md) — cómo correr, probar y empaquetar.

## Ramas

* `main` — estable, refleja lo desplegado.
* `develop` — integración. Todo feature se mergea aquí primero.
