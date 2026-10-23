import { Injectable, OnDestroy } from "@angular/core";
import { signal } from "@angular/core";
import { MsalService } from "@azure/msal-angular";
import { MsalBroadcastService } from "@azure/msal-angular";
import { EventType, InteractionStatus } from "@azure/msal-browser";
import { Subject, filter, takeUntil } from "rxjs";

/** Gestiona la autenticación y expone el estado de sesión de MSAL. */
@Injectable({
    providedIn: 'root'
})
export class AuthService implements OnDestroy {
    /** Indica si existe una sesión activa. */
    public authenticatedUser = signal<boolean>(false);

    /** Nombre del usuario autenticado, o null si no existe sesión. */
    public userName = signal<string | null>(null);

    /** Identificador único del usuario autenticado, o null si no existe */
    public userId = signal<string | null>(null);

    /** Indica si el usuario tiene el rol de administrador. */
    public isAdmin = signal<boolean>(false);

    /** Cancela las suscripciones al destruir el servicio. */
    private destroy$ = new Subject<void>();

    constructor(
        private msalService: MsalService,
        private msalBroadcastService: MsalBroadcastService
    ) {
        this.msalService.handleRedirectObservable()
        .pipe(takeUntil(this.destroy$))
        .subscribe();

        this.msalBroadcastService.inProgress$
            .pipe(
                filter((status) => status === InteractionStatus.None),
                takeUntil(this.destroy$)
            )
            .subscribe(() => {
                this.syncAccountState();
            });

        this.msalBroadcastService.msalSubject$
            .pipe(
                filter((msg) => msg.eventType === EventType.LOGIN_SUCCESS),
                takeUntil(this.destroy$)
            )
            .subscribe(() => {
                this.syncAccountState();
            });
    }

    /**
     * Actualiza el estado de sesión a partir de la cuenta activa
     */
    private syncAccountState(): void {
        const account = this.msalService.instance.getAllAccounts()[0];

        this.authenticatedUser.set(Boolean(account));
        this.userName.set(account?.name ?? null);

        const userId = account?.idTokenClaims?.['oid'];

        this.userId.set(typeof userId === 'string' ? userId : null);

        const roles = (account?.idTokenClaims?.['roles'] as string[]) ?? [];
        this.isAdmin.set(roles.includes('admin'));
    }

    /** Inicia el login mediante redirección a Microsoft Entra ID. */
    public login(): void {
        this.msalService.loginRedirect();
    }

    /** Cierra la sesión local y la sesión de Microsoft Entra ID. */
    public logOut(): void {
        this.msalService.logoutRedirect();
        this.authenticatedUser.set(false);
        this.userName.set(null);
        this.userId.set(null);
        this.isAdmin.set(false);
    }

    /** Libera las suscripciones del servicio. */
    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
    }
}