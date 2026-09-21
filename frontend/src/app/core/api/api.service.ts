import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private readonly base = 'https://y2fmjg2ed2.execute-api.us-east-1.amazonaws.com';

  constructor(private http: HttpClient) {}

  public getProducts(): Observable<any> {
    return this.http.get(`${this.base}/api/v1/products`);
  }

  public getOrders(): Observable<any> {
    return this.http.get(`${this.base}/api/v1/orders`);
  }
}