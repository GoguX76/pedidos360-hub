# Config JWT propuesta — Pedidos 360

> TAREA 2 — Alinear config JWT de Spring.
> Preparado por el equipo. **No se edita `application.properties` a ciegas:** primero se propone aqui, y se confirma contra el token real de la API (TAREA 3).

## 1) Contexto

Los microservicios `orders` y `products` (Spring Boot) validan el JWT de la API con la config de `spring.security.oauth2.resourceserver.jwt.*`. Para que el token sea aceptado, los campos `iss` (emisor) y `aud` (audiencia) del token deben coincidir con lo configurado.

## 2) Estado actual en el repo

| Servicio | Archivo | Estado |
|---|---|---|
| **products** | `backend/products/src/main/resources/application.properties` (lineas 5-6) | ✅ Ya tiene config JWT con defaults por variable de entorno |
| **orders** | `backend/orders/src/main/resources/application.properties` | ✅ Ya tiene la config JWT aplicada (mismo esquema que products) |

Detalle de products (ya presente):

```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=${AZURE_ISSUER_URI:https://login.microsoftonline.com/6a3978a5-1a22-4be4-bbb8-a7c6279c471e/v2.0}
spring.security.oauth2.resourceserver.jwt.audiences=${AZURE_CLIENT_ID:4d1afbc7-9d81-4ef4-a0e2-fd0ec724f8f4}
```

## 3) Config aplicada en orders

En `backend/orders/src/main/resources/application.properties`:

```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=${AZURE_ISSUER_URI:https://login.microsoftonline.com/6a3978a5-1a22-4be4-bbb8-a7c6279c471e/v2.0}
spring.security.oauth2.resourceserver.jwt.audiences=${AZURE_CLIENT_ID:4d1afbc7-9d81-4ef4-a0e2-fd0ec724f8f4}
```

`orders` queda igual que `products` (misma estrategia: variable de entorno con default), manteniendo la consistencia entre ambos microservicios.

## 4) Dependencias agregadas en orders

En `backend/orders/build.gradle`:

```gradle
implementation 'org.springframework.boot:spring-boot-starter-security'
implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'
```

Build local verificado: `cd backend\orders; .\gradlew.bat build -x test` → **BUILD SUCCESSFUL**.

## 5) Justificacion con los claims del token (pendiente de confirmacion)

Los valores propuestos se basan en el registro de la app en Entra ID:

| Valor | Base |
|---|---|
| `issuer-uri` = `https://login.microsoftonline.com/6a3978a5-1a22-4be4-bbb8-a7c6279c471e/v2.0` | El tenant de la organizacion es `6a3978a5-1a22-4be4-bbb8-a7c6279c471e`; los tokens de las apps se emiten desde ese dominio `/v2.0`. |
| `audiences` = `4d1afbc7-9d81-4ef4-a0e2-fd0ec724f8f4` | Es el clientId (app) registrado en Entra; el token de la API de Pedidos 360 debe traer ese `aud`. |

**Pendiente:** confirmar `iss` y `aud` exactos contra el token REAL de la API (TAREA 3). Ese token todavia no se pudo capturar porque la API `api://62985756-8182-4059-a3d6-2fadd8355b88` no existe en el tenant (error AADSTS500011 — requiere accion del lider en Entra ID). Cuando el lider lo pase, decodificar en https://jwt.ms y verificar que coincidan; si difieren, ajustar esta propuesta.

## 6) Nota sobre Maven vs Gradle

La TAREA 2 menciona `mvn -f backend/orders/pom.xml`. En el repo **no existe `pom.xml`**: los proyectos son **Gradle** (archivos `build.gradle`). Por eso el build se verifica con `gradlew.bat build`. Queda registrado para el lider en `docs/tareas-lider.md`.

## 7) Pendientes para el lider

1. Pasar el token REAL de la API (TAREA 3) para confirmar `iss`/`aud`.
2. Verificar/crear el App Registration de la API `api://62985756-8182-4059-a3d6-2fadd8355b88` en Entra ID (resuelve el 500011).
3. Aprobar oficialmente el cambio en `orders` (merge).