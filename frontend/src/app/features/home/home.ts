import { Component, inject, type OnInit } from '@angular/core';
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

  protected products: any[] = [];
  protected orders: any[] = [];
  protected loadingProducts = true;
  protected loadingOrders = true;
  protected errorProducts = '';
  protected errorOrders = '';

  public ngOnInit(): void {
    this.apiService.getProducts().subscribe({
      next: (data) => {
        this.products = this.toList(data, 'products');
        this.loadingProducts = false;
      },
      error: (err) => {
        this.errorProducts = this.readError(err);
        this.loadingProducts = false;
      },
    });

    this.apiService.getOrders().subscribe({
      next: (data) => {
        this.orders = this.toList(data, 'orders');
        this.loadingOrders = false;
      },
      error: (err) => {
        this.errorOrders = this.readError(err);
        this.loadingOrders = false;
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