import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import type { Product, ProductPayload } from './models/product.model';

@Injectable({
  providedIn: 'root',
})
export class ApiService {
  /**
   * Mismo origen (nginx proxea /api/* al gateway en prod y `ng serve`
   * lo proxea en dev vía proxy.conf.json). Así no hay CORS: el navegador
   * nunca hace preflight contra otro origen.
   */
  private readonly base = typeof window !== 'undefined' ? window.location.origin : '';

  constructor(private http: HttpClient) {}

  public getProducts(): Observable<Product[]> {
    return this.http.get<Product[]>(`${this.base}/api/v1/products`);
  }

  /**
   * Crea un producto. El backend exige rol ADMIN (ROLE_ADMIN): el token lo
   * adjunta el MsalInterceptor configurado en app.config.ts.
   */
  public createProduct(payload: ProductPayload): Observable<Product> {
    return this.http.post<Product>(`${this.base}/api/v1/products`, payload);
  }
}
