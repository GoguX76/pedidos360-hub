# Angular — Frontend

## Versión

Angular 22 con componentes standalone y signals (zoneless).

## Estructura

```
frontend/src/app/
├── app.ts / app.html / app.css    # Root component (layout con Header/Footer)
├── app.routes.ts                  # Rutas: /login, /home, /menu
├── app.config.ts                  # MSAL config (clientId, authority, scopes)
├── core/
│   ├── auth/auth.service.ts       # Servicio de autenticación MSAL
│   ├── api/api.service.ts         # Cliente HTTP (base URL dinámica)
│   ├── api/models/product.model.ts # Tipos del catálogo
│   └── catalog/categories.ts      # Categorías destacadas + helpers
├── features/
│   ├── login/                     # Página de login
│   ├── menu/                      # Catálogo con búsqueda, filtros y estados
│   └── home/                      # Página de inicio con categorías
└── shared/
    ├── layout/header/             # Header reutilizable
    ├── layout/footer/             # Footer reutilizable
    └── ui/nav-card/               # Tarjeta de navegación
```

## Características principales

- **Signals**: estado reactivo sin Zone.js (zoneless)
- **Tailwind CSS v4**: estilos con @theme (tokens de color, tipografía)
- **MSAL Angular**: login con Microsoft Entra ID
- **HttpClient**: llamadas a la API vía proxy (mismo origen, sin CORS)
- **Componentes standalone**: sin NgModules

## Autenticación

`app.config.ts` configura MSAL con:
- `clientId`: ID de la app registrada en Entra ID
- `authority`: URL del tenant
- `redirectUri`: `http://localhost:4200`
- Scopes: `openid profile` + `access_as_user` para la API

El `MsalInterceptor` adjunta el JWT automáticamente a las llamadas bajo `/api/v1/orders*`.

## API

`ApiService` usa `window.location.origin` como base URL para evitar CORS:
- Dev: `ng serve` con `proxy.conf.json` → redirige al gateway
- Prod: nginx en EC2 → redirige al gateway

## Tests

Vitest con tests unitarios para componentes y servicios.
