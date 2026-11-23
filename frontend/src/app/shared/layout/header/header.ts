import { Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { CartService } from '../../../core/cart/cart.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './header.html',
  styleUrl: './header.css',
})
export class HeaderComponent {
  protected authService = inject(AuthService);
  protected cartService = inject(CartService);
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