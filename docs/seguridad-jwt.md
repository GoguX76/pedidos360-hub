# Seguridad JWT — Pedidos 360 Hub

## Esquema

El frontend hace login con Microsoft Entra ID (MSAL) y obtiene un JWT.
Cada microservicio Spring actúa como **OAuth2 Resource Server** y valida el token
(`iss` + `aud`) en cada request. Sin JWT válido → `401`.

| Pieza | Valor |
|---|---|
| Tenant (Entra ID) | `6a3978a5-1a22-4be4-bbb8-a7c6279c471e` |
| Client ID (frontend) | `4d1afbc7-9d81-4ef4-a0e2-fd0ec724f8f4` |
| Scope de la API | `api://4d1afbc7-9d81-4ef4-a0e2-fd0ec724f8f4/access_as_user` |
| `aud` del token | `api://4d1afbc7-9d81-4ef4-a0e2-fd0ec724f8f4` |
| `iss` del token | `https://sts.windows.net/6a3978a5-1a22-4be4-bbb8-a7c6279c471e/` (token **v1**) |

> El registro emite tokens **v1** (`accessTokenAcceptedVersion`/`requestedAccessTokenVersion` = 1),
> por eso `iss` es `sts.windows.net` (no `/v2.0`) y `aud` es el URI `api://…` completo.

## Backend (orders y products, idéntico esquema)

`application.properties`:

```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=${AZURE_ISSUER_URI:https://login.microsoftonline.com/6a3978a5-1a22-4be4-bbb8-a7c6279c471e}
spring.security.oauth2.resourceserver.jwt.audiences=${AZURE_AUDIENCES:api://4d1afbc7-9d81-4ef4-a0e2-fd0ec724f8f4}
```

* `issuer-uri` usa la **discovery v1** (sin `/v2.0`); Spring obtiene de ahí el issuer
  real `http://sts.windows.net/<tenant>/` y valida la firma con esas claves.
* `audiences` es el URI completo `api://…` (coincide con el `aud` del token).

Dependencias (`build.gradle`): `spring-boot-starter-security`,
`spring-boot-starter-oauth2-resource-server`.

`SecurityConfig`:

* Sesión `STATELESS`, CSRF/login por formulario deshabilitados.
* Públicos: `OPTIONS /**` (preflight CORS) y `/actuator/health`, `/actuator/info`.
* Todo lo demás: autenticado.
* `JwtAuthenticationConverter` mapea el claim `roles` a autoridades `ROLE_*`.
* CORS por propiedad `app.cors.allowed-origins` (métodos GET/POST/PUT/PATCH/DELETE/OPTIONS).

En `orders`, el `POST /api/v1/orders` además extrae el `customerId` del claim
`oid` del token, por lo que el cliente no puede suplantar identidad.

## Frontend

`app.config.ts`:

* `MSALInstanceFactory`: `clientId`, `authority` (tenant) y `redirectUri`
  `http://localhost:4200` (debe coincidir con lo registrado en Azure).
* `MSALGuardConfigFactory`: `InteractionType.Redirect`, scopes `openid profile`.
* `MSALInterceptorConfigFactory`: el scope `access_as_user` se adjunta a las
  llamadas bajo `https://y2fmjg2ed2.execute-api.us-east-1.amazonaws.com/api/v1/orders*`.

## Variables de entorno (producción)

| Variable | Efecto |
|---|---|
| `AZURE_ISSUER_URI` | Sobrescribe el emisor JWT |
| `AZURE_AUDIENCES` | Sobrescribe la audiencia JWT |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Conexión PostgreSQL |
| `app.cors.allowed-origins` | Orígenes permitidos (propiedad Spring) |
