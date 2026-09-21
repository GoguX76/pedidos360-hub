# TAREAS PARA EL LIDER — Pedidos 360

> Avisos detectados por el equipo. Esas acciones **solo el lider** puede hacerlas (requieren AWS/Azure). Si algo de aqui no esta en su alcance, confirmar con el equipo.

## 1) Backend caido — API Gateway devuelve 503 (resuelto)

Probado el 2026-09-19 desde una maquina externa (sin AWS):

```
GET /api/v1/products            -> HTTP 503 {"message":"Service Unavailable"}
GET /api/v1/orders (sin token)  -> HTTP 503
GET /api/v1/orders (con token)  -> HTTP 503
```

Significado: `y2fmjg2ed2.execute-api.us-east-1.amazonaws.com` responde, pero **no podia conectar con los microservicios** (EC2 apagada).

- [x] Revisar/levantar la EC2 de backend — **resuelto** (el gateway ahora responde 200/401).
- [x] Verificar Security Group / rutas del gateway.

## 2) Frontend desplegado no respondia (EC2 caida) — resuelto

```
curl https://34.230.203.32  -> ERR_CONNECTION_TIMED_OUT (antes)
Test-NetConnection 34.230.203.32:443 -> True (ahora)
```

- [x] Verificar que la instancia frontend este encendida y con puerto 443 abierto — **resuelto**.

## 3) API Gateway sin CORS habilitado (pendiente)

Preflight OPTIONS (Origin: https://34.230.203.32) actual:

```
OPTIONS /api/v1/products -> HTTP 403 Invalid CORS request (sin Access-Control-Allow-Origin)
```

El gateway rechaza la Origin de la app desplegada. Resultado: las llamadas del navegador pueden fallar por CORS aunque el backend responda.

- [ ] Configurar CORS en el API Gateway (permitir Origin de la app: `https://34.230.203.32` y `http://localhost:4200`).

## 4) Token de API: la API no existe en Entra (TAREA 3) — BLOQUEADO

Al intentar en local, MSAL falla al obtener el token de la API con:

```
AADSTS500011: The resource principal named api://62985756-8182-4059-a3d6-2fadd8355b88
was not found in the tenant named 6a3978a5-1a22-4be4-bbb8-a7c6279c471e.
```

Significado: **el App Registration de la API no existe en el tenant** (o su Application ID URI es distinto). No es falta de consent: el recurso no existe.

Evidencia: Local Storage solo tiene token de Microsoft Graph (`aud=00000003-0000-0000-c000-000000000000`, `scp=openid profile email`). Ningun token con `access_as_user`.

- [ ] Verificar que existe el App Registration con URI `api://62985756-8182-4059-a3d6-2fadd8355b88` en el tenant `6a3978a5-1a22-4be4-bbb8-a7c6279c471e`.
- [ ] Si existe con otro URI/clientId, pasarnos el correcto y actualizar scope en `frontend/src/app/app.config.ts`.
- [ ] Exponer el scope `access_as_user` y, de ser necesario, dar admin consent.

## 5) Repo: orders con config JWT aplicada — aval de build (TAREA 2)

- Se agrego a `backend/orders/src/main/resources/application.properties`: `issuer-uri` y `audiences` (igual que products, con variable de entorno + default).
- Se agrego a `backend/orders/build.gradle`: `spring-boot-starter-security` y `spring-boot-starter-oauth2-resource-server`.
- Build local verificado: `.\gradlew.bat build -x test` → **BUILD SUCCESSFUL**.

- [ ] Aprobar oficialmente el cambio (merge) — sin su aprobacion no se pushea.
- [ ] Validar con token real cuando se resuelva el punto 4.

## 6) Repo: no hay pom.xml — es Gradle (TAREA 2)

La instruccion decia `mvn -f backend/orders/pom.xml package`. En el repo no hay `pom.xml` (los proyectos usan **Gradle**: `build.gradle` + `gradlew.bat`). Verificacion de build local:

```
cd backend/products -> .\gradlew.bat build -x test -> BUILD SUCCESSFUL
cd backend/orders    -> .\gradlew.bat build -x test -> BUILD SUCCESSFUL
cd backend/products  -> .\gradlew.bat test           -> BUILD SUCCESSFUL (tests OK)
```

- [ ] Confirmar si la entrega debe adaptarse a Gradle o si el equipo debe instalar Maven para el build.

---

**Estado general:** las tareas 1, 2 y 4 del equipo estan listas de su lado. Faltan estas acciones de infraestructura para poder completar TAREA 3 y TAREA 5.