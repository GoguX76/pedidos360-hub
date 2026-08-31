import { Injectable, OnDestroy } from "@angular/core";
import { signal } from "@angular/core";
import { MsalService } from "@azure/msal-angular";
import { MsalBroadcastService } from "@azure/msal-angular";
import { EventType,InteractionStatus } from "@azure/msal-browser";
import { Subject, filter, takeUntil } from "rxjs";

/**
 * Servicio encargado de gestionar la autenticación del usuario mediante
 * Microsoft Entra ID (MSAL), y de exponer su estado de sesión al resto
 * de la aplicación a través de un signal reactivo.
 *
 * @remarks
 * Este servicio escucha los eventos de MSAL en su constructor para
 * mantener `authenticatedUser` sincronizado automáticamente, en vez de
 * que cada componente tenga que consultar el estado de login por su cuenta.
 */

@Injectable({
    providedIn: 'root'
})
export class AuthService implements OnDestroy {
    /**
     * Indica si el usuario tiene una sesión activa.
     * Los componentes deben leer este signal (no consultar MSAL directamente)
     * para decidir qué mostrar en pantalla (ej. botón de login vs. datos del usuario).
     */
    public authenticatedUser = signal<boolean>(false);

    /**
     * Subject interno usado únicamente para cancelar las suscripciones
     * de este servicio en `ngOnDestroy`, evitando fugas de memoria.
     */
    private destroy$ = new Subject<void>();

    constructor(
        private msalService: MsalService,
        private msalBroadcastService: MsalBroadcastService
    ) {
        // MSAL requiere procesar la respuesta de Azure AD apenas el usuario
        // vuelve de la pantalla de login (contiene el código de autorización
        // en la URL). Sin esta suscripción, esa respuesta nunca se procesa.
        this.msalService.handleRedirectObservable()
        .pipe(takeUntil(this.destroy$))
        .subscribe();

        this.msalBroadcastService.inProgress$
            .pipe(
                filter((status) => status === InteractionStatus.None),
                takeUntil(this.destroy$)
            )
            .subscribe(() => {
                this.authenticatedUser.set(this.msalService.instance.getAllAccounts().length > 0);
            });

        this.msalBroadcastService.msalSubject$
            .pipe(
                // Filtramos porque msalSubject$ emite TODOS los eventos de MSAL
                // (inicio de login, error, logout, etc.), y solo nos interesa
                // reaccionar cuando el login se completó con éxito.
                filter((msg) => msg.eventType === EventType.LOGIN_SUCCESS),
                takeUntil(this.destroy$)
            )
            .subscribe(() => {
                this.authenticatedUser.set(true);
            });
    }

    /**
     * Inicia el flujo de login redirigiendo al usuario hacia la pantalla
     * de autenticación de Microsoft Entra ID.
     *
     * @remarks
     * Usa redirect (no popup) porque la app está registrada como SPA
     * con ese flujo en Azure AD.
     */
    public login(): void {
        this.msalService.loginRedirect();
    }

    /**
     * Cierra la sesión del usuario tanto en la aplicación como en Azure AD.
     */
    public logOut(): void {
        this.msalService.logoutRedirect();
        this.authenticatedUser.set(false);
    }

    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
    }
}