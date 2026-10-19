# Microsoft Entra ID — Autenticación

## Rol

Microsoft Entra ID es el proveedor de identidad. El frontend obtiene un JWT y los microservicios lo validan en cada request.

## Configuración

| Valor | Descripción |
|---|---|
| Tenant ID | `6a3978a5-1a22-4be4-bbb8-a7c6279c471e` |
| Client ID (frontend) | `4d1afbc7-9d81-4ef4-a0e2-fd0ec724f8f4` |
| Scope de la API | `api://4d1afbc7-9d81-4ef4-a0e2-fd0ec724f8f4/access_as_user` |
| Audience del token | `api://4d1afbc7-9d81-4ef4-a0e2-fd0ec724f8f4` |
| Issuer (token v1) | `https://sts.windows.net/6a3978a5-1a22-4be4-bbb8-a7c6279c471e/` |

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
