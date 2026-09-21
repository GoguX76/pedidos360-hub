# Desarrollo — Pedidos 360 Hub

## Requisitos

* JDK 25 (los `Dockerfile` usan `eclipse-temurin:25`).
* Node 20+ / npm 11 (`packageManager: npm@11.16.0`), Angular CLI 22.
* PostgreSQL solo para perfil `prod`.

## Correr en local

```powershell
# Orders (:8081) y products (:8082), perfil dev = H2 en memoria
cd backend/orders; .\gradlew.bat bootRun
cd backend/products; .\gradlew.bat bootRun

# Frontend (:4200)
cd frontend; npm install; npm start
```

Salud: `http://localhost:8081/actuator/health`,
`http://localhost:8082/actuator/health`. Consola H2 (solo dev):
`http://localhost:8081/h2-console` (JDBC `jdbc:h2:mem:pedidos360`).

## Tests y build

```powershell
cd backend/orders; .\gradlew.bat test        # tests (JUnit)
cd backend/orders; .\gradlew.bat build       # build completo
cd backend/orders; .\gradlew.bat build -x test  # solo empaquetado
cd frontend; npm test                        # tests (vitest)
cd frontend; npm run build                   # build producción
```

## Perfiles

Perfil activo por defecto: `dev` (`spring.profiles.active=dev`).
Para producción, activar `prod` y definir `DB_URL`, `DB_USER`, `DB_PASSWORD`
(ver [`seguridad-jwt.md`](seguridad-jwt.md) para `AZURE_*` y CORS).

## Docker

```powershell
cd backend/orders; docker build -t pedidos360-orders .
docker run -p 8081:8081 pedidos360-orders
# Igual para products con -p 8082:8082
```

## Flujo de ramas

1. Feature desde `develop` → PR a `develop`.
2. `develop` → `main` (estable/desplegado) solo con merge revisado.
