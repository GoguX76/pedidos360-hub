import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { CATALOG_CATEGORIES } from '../../core/catalog/categories';

/**
 * Página de inicio (pública). Muestra el hero de presentación y una
 * grilla de categorías destacadas que llevan al menú filtrado.
 * La barra superior la aporta el AppShell (header compartido).
 */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class LoginComponent {
  protected authService = inject(AuthService);
  protected readonly categories = CATALOG_CATEGORIES;

  protected onLoginClick(): void {
    this.authService.login();
  }
}
