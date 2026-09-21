# Registrar la API en Entra ID (solución AADSTS500011) — Pedidos 360 Hub

## El problema

El frontend pide un token para `api://62985756-8182-4059-a3d6-2fadd8355b88/access_as_user`,
pero ese recurso **no existe** en el tenant `6a3978a5-1a22-4be4-bbb8-a7c6279c471e`.
Entra responde `AADSTS500011: The resource principal named api://… was not found`.
Esto **no** se arregla prendiendo EC2 ni redeplegando: lo debe hacer un
administrador en el portal de Azure. Solo el login funciona hoy porque el App
Registration del frontend (`4d1afbc7-…`) sí existe.

## Pasos (solo admin, portal de Azure → Microsoft Entra ID)

### 1. Crear el App Registration de la API

1. **App registrations → New registration**.
   * Name: `Pedidos360 API`.
   * Supported account types: la misma opción que usó el frontend
     (single tenant de la organización, salvo que el frontend sea multitenant).
   * Redirect URI: dejar vacío (es una API, no hace login).
2. Anotar su **Application (client) ID**.

### 2. Definir su Application ID URI

1. En el registro nuevo: **Expose an API → Add** (Application ID URI).
2. Debe quedar exactamente:
   `api://62985756-8182-4059-a3d6-2fadd8355b88`
   (es el valor que ya usan `app.config.ts` y los `audiences` del backend).
   * Si el ID de la app creada es distinto, avisar al equipo: hay que actualizar
     el scope en `frontend/src/app/app.config.ts` y `audiences` en ambos
     `application.properties`.

### 3. Exponer el scope `access_as_user`

1. **Expose an API → Add a scope**:
   * Scope name: `access_as_user`.
   * Who can consent: `Admins and users`.
   * Admin consent display name / description: ej. `Acceso a la API de Pedidos 360`.
   * State: `Enabled`.
2. El scope completo queda:
   `api://62985756-8182-4059-a3d6-2fadd8355b88/access_as_user` (ya referenciado en el frontend).

### 4. Autorizar el frontend en la API

1. **Expose an API → Authorized client applications → Add a client application**:
   * Client ID: `4d1afbc7-9d81-4ef4-a0e2-fd0ec724f8f4` (el frontend).
   * Marcar el scope `access_as_user`.
2. En el registro del **frontend** (**API permissions → Add a permission →
   My APIs → Pedidos360 API**): agregar `access_as_user` y pulsar
   **Grant admin consent**.

### 5. Registrar las Redirect URIs del frontend (login en prod y local)

En el registro del frontend (**Authentication → Add a platform → Single-page
application**), agregar ambas:

* `https://34.230.203.32` y `https://34.230.203.32/login`
* `http://localhost:4200` y `http://localhost:4200/login` (desarrollo)

Sin esto, el login en la EC2 falla aunque el token de API ya funcione
(el código ahora usa `window.location.origin`, por eso ambas deben existir).

## Verificación

1. Entrar a `https://34.230.203.32/login`, iniciar sesión y llamar un endpoint
   (ej. `GET /api/v1/products` vía gateway).
2. Si falla, copiar el access token (DevTools → Application → Local Storage) y
   pegarlo en `https://jwt.ms`. Debe mostrar:
   * `aud`: `api://62985756-8182-4059-a3d6-2fadd8355b88` (si sale
     `00000003-0000-0000-c000-000000000000` es token de Graph, no de la API).
   * `iss`: `https://login.microsoftonline.com/6a3978a5-1a22-4be4-bbb8-a7c6279c471e/v2.0`.
   * `scp`: incluye `access_as_user`.
3. El backend valida `iss` + `aud` (`seguridad-jwt.md`); si el gateway tiene su
   propio authorizer JWT, su audiencia esperada debe ser la misma.
