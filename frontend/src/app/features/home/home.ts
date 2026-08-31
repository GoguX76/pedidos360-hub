import { Component, inject } from '@angular/core';
import { AuthService } from '../../core/auth/auth.service';
import { NavCardComponent } from '../../shared/ui/nav-card/nav-card';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [NavCardComponent],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home {
  protected authService = inject(AuthService);

  protected onLogoutClick(): void {
    this.authService.logOut();
  }
}