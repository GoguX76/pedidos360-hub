import { ApplicationConfig, provideBrowserGlobalErrorListeners, importProvidersFrom } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HTTP_INTERCEPTORS } from '@angular/common/http';
import {
  IPublicClientApplication,
  PublicClientApplication,
  InteractionType,
  BrowserCacheLocation
} from '@azure/msal-browser';
import {
  MsalModule,
  MsalService,
  MsalGuard,
  MsalBroadcastService,
  MsalInterceptor,
  MsalGuardConfiguration,
  MsalInterceptorConfiguration,
  ProtectedResourceScopes
} from '@azure/msal-angular';

import { routes } from './app.routes';

/** Devuelve el origen de la aplicación en el entorno actual. */
function appOrigin(): string {
  return typeof window !== 'undefined' ? window.location.origin : 'http://localhost:4200';
}

/** Crea la instancia principal de MSAL. */
export function MSALInstanceFactory(): IPublicClientApplication {
  const origin = appOrigin();
  return new PublicClientApplication({
    auth: {
      clientId: '4d1afbc7-9d81-4ef4-a0e2-fd0ec724f8f4',
      authority: 'https://login.microsoftonline.com/6a3978a5-1a22-4be4-bbb8-a7c6279c471e',
      redirectUri: origin,
      postLogoutRedirectUri: `${origin}/login`
    },
    cache: {
      cacheLocation: BrowserCacheLocation.LocalStorage
    }
  });
}

/** Configura el flujo de autenticación de las rutas protegidas. */
export function MSALGuardConfigFactory(): MsalGuardConfiguration {
  return {
    interactionType: InteractionType.Redirect,
    authRequest: {
      scopes: ['openid', 'profile']
    }
  };
}

/** Configura las rutas y scopes gestionados por el interceptor de MSAL. */
export function MSALInterceptorConfigFactory(): MsalInterceptorConfiguration {
  const protectedResourceMap = new Map<
    string,
    Array<string | ProtectedResourceScopes> | null
  >();
  const apiScope = 'api://4d1afbc7-9d81-4ef4-a0e2-fd0ec724f8f4/access_as_user';
  const api = appOrigin();

  protectedResourceMap.set(`${api}/api/v1/products*`, [
    { httpMethod: 'GET', scopes: null },
    { httpMethod: 'POST', scopes: [apiScope] },
    { httpMethod: 'PUT', scopes: [apiScope] },
    { httpMethod: 'PATCH', scopes: [apiScope] },
    { httpMethod: 'DELETE', scopes: [apiScope] }
  ]);

  protectedResourceMap.set(`${api}/api/v1/orders*`, [
    { httpMethod: 'GET', scopes: null },
    { httpMethod: 'POST', scopes: [apiScope] },
    { httpMethod: 'PATCH', scopes: [apiScope] },
    { httpMethod: 'DELETE', scopes: [apiScope] }
  ]);

  return {
    interactionType: InteractionType.Redirect,
    protectedResourceMap
  };
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),

    provideHttpClient(withInterceptorsFromDi()),

    importProvidersFrom(MsalModule.forRoot(
      MSALInstanceFactory(),
      MSALGuardConfigFactory(),
      MSALInterceptorConfigFactory()
    )),

    {
      provide: HTTP_INTERCEPTORS,
      useClass: MsalInterceptor,
      multi: true
    },

    MsalService,
    MsalGuard,
    MsalBroadcastService
  ]
};