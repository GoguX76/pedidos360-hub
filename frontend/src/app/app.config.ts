import { ApplicationConfig, provideBrowserGlobalErrorListeners, importProvidersFrom } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HTTP_INTERCEPTORS } from '@angular/common/http';
import {
  IPublicClientApplication,
  PublicClientApplication,
  InteractionType,
  BrowserCacheLocation,
  LogLevel
} from '@azure/msal-browser';
import {
  MsalModule,
  MsalService,
  MsalGuard,
  MsalBroadcastService,
  MsalInterceptor,
  MSAL_INSTANCE,
  MSAL_GUARD_CONFIG,
  MSAL_INTERCEPTOR_CONFIG,
  MsalGuardConfiguration,
  MsalInterceptorConfiguration
} from '@azure/msal-angular';

import { routes } from './app.routes';

/**
 * Crea la instancia principal de MSAL, usando los datos de tu
 * App Registration (clientId y tenant) que ya configuraste en Azure.
 */
export function MSALInstanceFactory(): IPublicClientApplication {
  return new PublicClientApplication({
    auth: {
      clientId: '4d1afbc7-9d81-4ef4-a0e2-fd0ec724f8f4', // Id. de aplicación (cliente)
      authority: 'https://login.microsoftonline.com/6a3978a5-1a22-4be4-bbb8-a7c6279c471e', // Id. del Inquilino (Tenant)
      redirectUri: 'http://localhost:4200', // La URL debe COINCIDIR con la que usa Azure
      postLogoutRedirectUri: 'http://localhost:4200/login' // NUEVA línea: a dónde volver tras cerrar sesión
    },
    cache: {
      cacheLocation: BrowserCacheLocation.LocalStorage // El token sobrevive si recargas la página
    }
  });
}

/**
 * Configura qué hace el MsalGuard cuando una ruta protegida detecta
 * que el usuario no tiene sesión activa.
 */
export function MSALGuardConfigFactory(): MsalGuardConfiguration {
  return {
    interactionType: InteractionType.Redirect, // mismo flujo que usa tu AuthService
    authRequest: {
      scopes: ['openid', 'profile'] // scopes básicos; el de tu API se agrega después
    }
  };
}

/**
 * Configura el interceptor que adjunta el JWT automáticamente
 * en cada llamada HTTP hacia las URLs que definas aquí.
 */
export function MSALInterceptorConfigFactory(): MsalInterceptorConfiguration {
  const protectedResourceMap = new Map<string, Array<string> | null>();

  protectedResourceMap.set('http://localhost:8080/*', ['api://62985756-8182-4059-a3d6-2fadd8355b88/access_as_user']);

  return {
    interactionType: InteractionType.Redirect,
    protectedResourceMap
  };
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),

    // Necesario para que el interceptor de MSAL funcione con HttpClient.
    provideHttpClient(withInterceptorsFromDi()),

    // Registra MsalModule y sus servicios (Service, Guard, Broadcast).
    importProvidersFrom(MsalModule.forRoot(
      MSALInstanceFactory(),
      MSALGuardConfigFactory(),
      MSALInterceptorConfigFactory()
    )),

    // El interceptor en sí, registrado como un HTTP_INTERCEPTOR más.
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