import { Component, inject, signal, type OnInit } from '@angular/core';
import { Location } from '@angular/common';
import { AuthService } from '../../core/auth/auth.service';
import { ApiService } from '../../core/api/api.service';

/**
 * Página de Menú/Productos.
 * Se accede desde el link "Menú" en login. Al entrar, carga la lista
 * de productos desde la API y los muestra. El botón "Volver" regresa
 * a la página anterior (equivalente al botón "atrás" del navegador).
 */
@Component({
  selector: 'app-menu',
  standalone: true,
  imports: [],
  templateUrl: './menu.html',
  styleUrl: './menu.css'
})
export class MenuComponent implements OnInit {
  // Servicio de autenticación (MSAL). Se mantiene inyectado por si esta
  // página necesita saber si hay un usuario logueado.
  protected authService = inject(AuthService);

  // Servicio que hace las llamadas HTTP al backend para traer productos.
  protected apiService = inject(ApiService);

  // Location de Angular: permite navegar "hacia atrás" en el historial
  // del navegador, sin depender de una ruta fija.
  private location = inject(Location);

  // Signals: la app es zoneless (Angular sin zone.js), así que solo
  // los signals disparan change detection cuando llega la respuesta
  // async de la API. Usar campos planos aquí dejaría la vista colgada.

  // Lista de productos ya cargados desde la API.
  protected products = signal<any[]>([]);

  // true mientras se está esperando la respuesta de la API.
  protected loadingProducts = signal(true);

  // Mensaje de error si la carga de productos falla (ej. "HTTP 503").
  protected errorProducts = signal('');

  /**
   * Se ejecuta automáticamente al entrar a la página.
   * Pide los productos a la API y actualiza los signals según el resultado.
   */
  public ngOnInit(): void {
    this.apiService.getProducts().subscribe({
      next: (data) => {
        // Éxito: normalizamos la respuesta a un array y dejamos de "cargando".
        this.products.set(this.toList(data, 'products'));
        this.loadingProducts.set(false);
      },
      error: (err) => {
        // Error: guardamos un mensaje legible y dejamos de "cargando".
        this.errorProducts.set(this.readError(err));
        this.loadingProducts.set(false);
      },
    });
  }

  /**
   * Normaliza la respuesta de la API a un array de productos.
   * Soporta dos formas de respuesta:
   * - Un array directo: [ {...}, {...} ]
   * - Un objeto con la key indicada: { products: [ {...}, {...} ] }
   */
  private toList(data: any, key: string): any[] {
    if (Array.isArray(data)) return data;
    if (data && Array.isArray(data[key])) return data[key];
    return [];
  }

  /**
   * Convierte un error HTTP/JS en un texto corto para mostrar al usuario.
   * Ej: "HTTP 503" si viene de una respuesta HTTP, o el mensaje del error
   * si es otro tipo de falla (ej. red caída).
   */
  private readError(err: any): string {
    return err?.status ? `HTTP ${err.status}` : err?.message ?? 'Error desconocido';
  }

  /**
   * Acción del botón "Volver". Regresa a la página anterior en el
   * historial del navegador (como el botón "atrás").
   */
  protected onBackClick(): void {
    this.location.back();
  }
}