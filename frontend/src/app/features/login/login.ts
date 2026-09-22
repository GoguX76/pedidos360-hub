import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

/**
 * Pantalla de inicio de sesión. Muestra un botón que dispara el
 * flujo de login vía MSAL, o el nombre del usuario si ya tiene una
 * sesión activa. El link "Menú" navega a la página de productos.
 */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class LoginComponent {
  protected authService = inject(AuthService);

  protected showUserMenu = signal(false);

  protected onUserClick(): void {
    this.showUserMenu.set(!this.showUserMenu());
  }

  protected onLoginClick(): void {
    this.authService.login();
  }

  protected onLogoutClick(): void {
    this.authService.logOut();
  }
}