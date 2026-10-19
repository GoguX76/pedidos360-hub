import { Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';

/**
 * Barra de navegación superior compartida por toda la app.
 * Reemplaza la navegación que antes estaba duplicada en login y menu.
 */
@Component({
  selector: 'app-header',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './header.html',
  styleUrl: './header.css',
})
export class HeaderComponent {
  protected authService = inject(AuthService);
  protected showUserMenu = signal(false);

  protected toggleUserMenu(): void {
    this.showUserMenu.update((open) => !open);
  }

  protected onLoginClick(): void {
    this.authService.login();
  }

  protected onLogoutClick(): void {
    this.showUserMenu.set(false);
    this.authService.logOut();
  }
}
