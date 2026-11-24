# Angular — Frontend

## Versión

Angular 22 con componentes standalone y signals (zoneless).

## Estructura

```
frontend/src/app/
├── app.ts / app.html / app.css    # Root component (layout con Header/Footer)
├── app.routes.ts                  # Rutas: /login, /menu, /carrito
├── app.config.ts                  # MSAL config (clientId, authority, scopes)
├── core/
│   ├── auth/auth.service.ts       # Servicio de autenticación MSAL
│   ├── api/api.service.ts         # Cliente HTTP (base URL dinámica)
│   ├── api/models/                # Tipos de producto y pedido
│   ├── cart/cart.service.ts       # Estado del carrito (signals)
│   └── catalog/categories.ts      # Categorías destacadas + helpers
├── features/
│   ├── login/                     # Página de login
│   ├── menu/                      # Catálogo con búsqueda, filtros y estados
│   └── cart/                      # Vista del carrito
└── shared/
    ├── directives/                # Directivas reutilizables
    ├── layout/header/             # Header reutilizable
    ├── layout/footer/             # Footer reutilizable
    └── ui/nav-card/               # Tarjeta de navegación
```

## Características principales

- **Signals**: estado reactivo sin Zone.js (zoneless), incluyendo el carrito
- **Tailwind CSS v4**: estilos con @theme (tokens de color, tipografía)
- **MSAL Angular**: login con Microsoft Entra ID
- **HttpClient**: llamadas a la API vía proxy (mismo origen, sin CORS)
- **Componentes standalone**: sin NgModules
- **Carrito de compras**: gestión local del carrito con señales (sin persistencia en servidor)

## Autenticación

`app.config.ts` configura MSAL con:
- `clientId`: ID de la app registrada en Entra ID (configurar por entorno)
- `authority`: URL del tenant de Entra ID (configurar por entorno)
- `redirectUri`: origen actual de la aplicación
- Scopes: `openid profile` + `access_as_user` para la API

El `MsalInterceptor` adjunta el JWT automáticamente a las llamadas protegidas bajo
`/api/v1/products*` y `/api/v1/orders*`. Las operaciones de lectura del catálogo
no requieren scope de la API; las operaciones de escritura sí.

## API

`ApiService` usa `window.location.origin` como base URL para evitar CORS:
- Dev: `ng serve` con `proxy.conf.json` → redirige al gateway
- Prod: nginx en EC2 → redirige al gateway

## Tests

Vitest con tests unitarios para componentes y servicios.
