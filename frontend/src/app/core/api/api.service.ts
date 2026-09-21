import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  /**
   * Mismo origen (nginx proxea /api/* al gateway en prod y `ng serve`
   * lo proxea en dev vía proxy.conf.json). Así no hay CORS: el navegador
   * nunca hace preflight contra otro origen.
   */
  private readonly base =
    typeof window !== 'undefined' ? window.location.origin : '';

  constructor(private http: HttpClient) {}

  public getProducts(): Observable<any> {
    return this.http.get(`${this.base}/api/v1/products`);
  }

  public getOrders(): Observable<any> {
    return this.http.get(`${this.base}/api/v1/orders`);
  }
}