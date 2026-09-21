# Registrar la API en Entra ID (solución AADSTS500011) — Pedidos 360 Hub

## Decisión: un solo App Registration

Se reutiliza `Pedidos-App` (`4d1afbc7-9d81-4ef4-a0e2-fd0ec724f8f4`) como frontend
**y** API. No hace falta crear otro registro. El scope que ya existe,
`api://4d1afbc7-9d81-4ef4-a0e2-fd0ec724f8f4/access_as_user`, es el que pide el
frontend (`app.config.ts`). El antiguo `api://62985756-…` no existe en el tenant
y era la causa del `AADSTS500011`.

> Detalle técnico: el backend valida `audiences = 4d1afbc7-…` (GUID sin prefijo).
> Eso corresponde al `aud` de un token **v1**. Por eso el manifiesto debe llevar
> `accessTokenAcceptedVersion: 1` (punto 1). Con tokens v2 el `aud` sería el URI
> `api://…` completo y el backend lo rechazaría.

## Pasos (solo admin, portal de Azure → Microsoft Entra ID → Pedidos-App)

### 1. Tokens v1 (obligatorio para que el backend acepte el `aud`)

1. **Manifiesto** → buscar `"accessTokenAcceptedVersion"`.
2. Si vale `null`, cambiarlo a `1` → **Save**.

### 2. Pre-autorizar el frontend (evita el prompt de consentimiento)

1. **Expose an API → Authorized client applications → Add a client application**.
2. Client ID: `4d1afbc7-9d81-4ef4-a0e2-fd0ec724f8f4` (la propia app).
3. Marcar `access_as_user` → Add.

### 3. Permiso + admin consent

1. **API permissions → Add a permission → My APIs → Pedidos-App**.
2. Marcar `access_as_user` (delegado) → Add permissions.
3. **Grant admin consent for…** → Yes.

### 4. Redirect URIs del frontend (login en prod y local)

**Authentication → Add a platform → Single-page application**, agregar:

* `https://34.230.203.32` y `https://34.230.203.32/login`
* `http://localhost:4200` y `http://localhost:4200/login`

(El código usa `window.location.origin`, por eso ambas deben existir.)

## Verificación

1. Login en `https://34.230.203.32/login` y llamar `GET /api/v1/products` vía gateway.
2. Copiar el access token (DevTools → Application → Local Storage) y pegarlo en
   `https://jwt.ms`. Debe mostrar:
   * `aud`: `4d1afbc7-9d81-4ef4-a0e2-fd0ec724f8f4` (si sale `api://…` es v2:
     volver al punto 1).
   * `iss`: `https://login.microsoftonline.com/6a3978a5-1a22-4be4-bbb8-a7c6279c471e/v2.0`.
   * `scp`: incluye `access_as_user`.
3. Si el gateway tiene su propio authorizer JWT, su audiencia esperada debe ser
   el mismo GUID `4d1afbc7-…`.
