import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import type { Product, ProductPayload } from './models/product.model';
import type { Order, OrderPayload } from './models/order.model';

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

  /**
   * Obtiene todos los productos disponibles en el catálogo
   * @returns Observable con la lista de productos
   */
  public getProducts(): Observable<Product[]> {
    return this.http.get<Product[]>(
      `${this.base}/api/v1/products`
    );
  }

  /**
   * Crea un nuevo producto en el catálogo
   * @param payload Datos del producto que se enviarán al backend
   * @returns Observable con el producto creado
   */
  public createProduct(payload: ProductPayload): Observable<Product> {
    return this.http.post<Product>(
      `${this.base}/api/v1/products`,
      payload,
    );
  }

  /**
   * Crea un pedido para el cliente autenticado
   *
   * El identificador del cliente no se envía en el payload:
   * el backend lo obtiene desde JWT
   *
   * @param payload Datos del pedido y sus productos
   * @returns Observable con el pedido creado
   */
  public createOrder(payload: OrderPayload): Observable<Order> {
    return this.http.post<Order>(
      `${this.base}/api/v1/orders`,
      payload,
    );
  }

  /**
   * Obtiene los pedidos asociados a un cliente
   * @param customerId Identificador único del cliente
   * @returns Observable con la lista de pedidos del cliente asociado
   */
  public getOrdersByCustomer(customerId: string): Observable<Order[]> {
    return this.http.get<Order[]>(
      `${this.base}/api/v1/orders/customer/${encodeURIComponent(customerId)}`,
    );
  }

  /**
   * Obtiene un pedido mediante su identificador
   * @param orderId Identificador único del pedido
   * @returns Observable con los datos completos del pedido
   */
  public getOrderById(orderId: number): Observable<Order> {
    return this.http.get<Order>(
      `${this.base}/api/v1/orders/${orderId}`,
    );
  }

  /**
   * Cancela un pedido existente
   *
   * El backend solo permite cancelar pedidos que todavía estén
   * en estado CREATED
   *
   * @param orderId Identificador único del pedido que se desea cancelar
   * @returns Observable con el pedido actualizado
   */
  public cancelOrder(orderId: number): Observable<Order> {
    return this.http.delete<Order>(
      `${this.base}/api/v1/orders/${orderId}`,
    );
  }
}
