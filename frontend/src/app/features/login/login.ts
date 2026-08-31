import { Component, effect,inject } from '@angular/core';
import { AuthService } from '../../core/auth/auth.service';
import { Router } from '@angular/router';
/**
 * Pantalla de inicio de sesión. Muestra un botón que dispara el
 * flujo de login vía MSAL, o un mensaje de bienvenida si el
 * usuario ya tiene una sesión activa.
 */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [], // vacío por ahora: no usamos ningún otro componente/directiva de Angular aquí
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class LoginComponent {
  // inject() nos permite pedirle a Angular el AuthService sin
  // necesidad de declararlo en un constructor.
  protected authService = inject(AuthService);
  private router = inject(Router);

  constructor() {
    effect(() => {
      if (this.authService.authenticatedUser()) {
        this.router.navigate(['/home']);
      }
    });
  }

  /**
   * Dispara el flujo de login redirigiendo a Microsoft.
   * La lógica real vive en AuthService — este componente solo
   * reacciona al clic del usuario.
   */
  protected onLoginClick(): void {
    this.authService.login();
  }

  /**
   * Cierra la sesión del usuario, tanto en la app como en Azure AD.
   * La lógica real vive en AuthService — este método solo reacciona al clic.
   */
  protected onLogoutClick(): void {
    this.authService.logOut();
  }
}