// import { Injectable } from "@angular/core";
// import { Signal, signal } from "@angular/core";
// import { MsalService } from "@azure/msal-angular";
// import { MsalBroadcastService } from "@azure/msal-angular";
// import { EventType, InteractionStatus } from "@azure/msal-browser";
// import { Subject, filter, takeUntil } from "rxjs";

// @Injectable({
//     providedIn: 'root'
// })
// export class AuthService {
//     public authenticatedUser = signal<boolean>(false);
//     private destroy$ = new Subject<void>();

//     constructor(
//         private msalService: MsalService,
//         private msalBroadcastService: MsalBroadcastService
//     ) {
//         this.msalService.handleRedirectObservable()
//         this.msalBroadcastService.inProgress$
//     }

//     public login(): void {
//         /*
//         Función para ir a página login
//         */
//     }

//     public logOut(): void {
//         /*
//         Función para cerrar sesión
//         */
//     }
// }