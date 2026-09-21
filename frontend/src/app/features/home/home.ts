import { Component, inject, signal, type OnInit } from '@angular/core';
import { AuthService } from '../../core/auth/auth.service';
import { ApiService } from '../../core/api/api.service';
import { NavCardComponent } from '../../shared/ui/nav-card/nav-card';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [NavCardComponent],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home implements OnInit {
  protected authService = inject(AuthService);
  protected apiService = inject(ApiService);

  // Signals (no campos planos): la app es zoneless (Angular 22 sin zone.js)
  // y solo los signals disparan change detection tras callbacks async.
  // Con campos planos + interceptor MSAL la vista quedaba colgada en
  // "Cargando..." aunque la data ya habia llegado.
  protected products = signal<any[]>([]);
  protected loadingProducts = signal(true);
  protected errorProducts = signal('');

  public ngOnInit(): void {
    this.apiService.getProducts().subscribe({
      next: (data) => {
        this.products.set(this.toList(data, 'products'));
        this.loadingProducts.set(false);
      },
      error: (err) => {
        this.errorProducts.set(this.readError(err));
        this.loadingProducts.set(false);
      },
    });
  }

  private toList(data: any, key: string): any[] {
    if (Array.isArray(data)) return data;
    if (data && Array.isArray(data[key])) return data[key];
    return [];
  }

  private readError(err: any): string {
    return err?.status ? `HTTP ${err.status}` : err?.message ?? 'Error desconocido';
  }

  protected onLogoutClick(): void {
    this.authService.logOut();
  }
}
