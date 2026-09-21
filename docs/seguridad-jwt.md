# Seguridad JWT — Pedidos 360 Hub

## Esquema

El frontend hace login con Microsoft Entra ID (MSAL) y obtiene un JWT.
Cada microservicio Spring actúa como **OAuth2 Resource Server** y valida el token
(`iss` + `aud`) en cada request. Sin JWT válido → `401`.

| Pieza | Valor |
|---|---|
| Tenant (Entra ID) | `6a3978a5-1a22-4be4-bbb8-a7c6279c471e` |
| Client ID (frontend) | `4d1afbc7-9d81-4ef4-a0e2-fd0ec724f8f4` |
| Scope de la API | `api://62985756-8182-4059-a3d6-2fadd8355b88/access_as_user` |

## Backend (orders y products, idéntico esquema)

`application.properties`:

```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=${AZURE_ISSUER_URI:https://login.microsoftonline.com/6a3978a5-1a22-4be4-bbb8-a7c6279c471e/v2.0}
spring.security.oauth2.resourceserver.jwt.audiences=${AZURE_CLIENT_ID:4d1afbc7-9d81-4ef4-a0e2-fd0ec724f8f4}
```

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
| `AZURE_CLIENT_ID` | Sobrescribe la audiencia JWT |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Conexión PostgreSQL |
| `app.cors.allowed-origins` | Orígenes permitidos (propiedad Spring) |
