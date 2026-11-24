# Microsoft Entra ID — Autenticación

## Rol

Microsoft Entra ID es el proveedor de identidad. El frontend obtiene un JWT y los microservicios lo validan en cada request.

## Configuración

| Valor | Descripción |
|---|---|
| Tenant ID | Configurar por entorno (`AZURE_TENANT_ID`) |
| Client ID (frontend) | Configurar por entorno (`AZURE_CLIENT_ID`) |
| Scope de la API | `api://<client-id>/access_as_user` |
| Audience del token | `api://<client-id>` |
| Issuer (token v1) | `https://sts.windows.net/<tenant-id>/` |

## Flujo de autenticación

```
1. Usuario → Frontend (Angular) → Redirige a Entra ID
2. Entra ID → Login → Emite JWT v1
3. Frontend recibe JWT → Lo adjunta en cada request a la API
4. Microservicios validan JWT (firma + issuer + audience)
5. JWT inválido → 401
```

## Token v1 vs v2

El registro emite tokens **v1** (`accessTokenAcceptedVersion` = 1):
- `iss`: `https://sts.windows.net/<tenant>/` (no `/v2.0`)
- `aud`: URI completo `api://...`

Por eso se usa un `JwtDecoder` propio en vez de `issuer-uri` de Spring.

## Seguridad en backend

- **Sesión STATELESS**: sin sesión de servidor
- **CSRF deshabilitado**: no aplica sin cookies
- **JwtAuthenticationConverter**: mapea claim `roles` a autoridades `ROLE_*`
- **Sin JWT válido → 401** en todos los endpoints (salvo actuator health/info)
