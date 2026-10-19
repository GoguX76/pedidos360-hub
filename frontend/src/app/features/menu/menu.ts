import { Component, computed, inject, signal, type OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ClickOutsideDirective } from '../../shared/directives/click-outside.directive';
import { AuthService } from '../../core/auth/auth.service';
import { ApiService } from '../../core/api/api.service';
import type { Product } from '../../core/api/models/product.model';
import {
  CATALOG_CATEGORIES,
  categoryIcon,
  categorySlug,
} from '../../core/catalog/categories';

/** Nivel de stock derivado, usado para colorear el indicador. */
type StockLevel = 'out' | 'low' | 'ok';

/**
 * Página de Menú/Productos. Carga el catálogo desde la API y lo muestra
 * en una grilla de tarjetas con buscador, filtro por categoría y un
 * toggle de "solo disponibles". Acepta `?categoria=<slug>` para llegar
 * ya filtrado desde las tarjetas de la página de inicio.
 */
@Component({
  selector: 'app-menu',
  standalone: true,
  imports: [RouterLink, ClickOutsideDirective],
  templateUrl: './menu.html',
  styleUrl: './menu.css',
})
export class MenuComponent implements OnInit {
  protected authService = inject(AuthService);
  protected apiService = inject(ApiService);
  private route = inject(ActivatedRoute);

  // Helpers expuestos a la plantilla.
  protected readonly iconFor = categoryIcon;

  // Catálogo y estado de carga.
  protected products = signal<Product[]>([]);
  protected loadingProducts = signal(true);
  protected errorProducts = signal('');

  // Filtros.
  protected search = signal('');
  protected categoryFilter = signal('todas');
  protected onlyAvailable = signal(false);

  // Menú de administrador.
  protected adminMenuOpen = signal(false);

  /** Opciones del selector: categorías destacadas + las que traiga la API. */
  protected readonly categoryOptions = computed(() => {
    const options = CATALOG_CATEGORIES.map((c) => ({ id: c.id, name: c.name }));
    const seen = new Set(options.map((o) => o.id));
    for (const p of this.products()) {
      const raw = (p.category ?? '').trim();
      if (!raw) continue;
      const id = categorySlug(raw);
      if (!id || seen.has(id)) continue;
      seen.add(id);
      options.push({ id, name: raw });
    }
    return options;
  });

  /** Productos tras aplicar buscador, categoría y disponibilidad. */
  protected readonly filteredProducts = computed(() => {
    const query = this.search().trim().toLowerCase();
    const category = this.categoryFilter();
    const onlyAvailable = this.onlyAvailable();

    return this.products().filter((p) => {
      if (onlyAvailable && !p.available) return false;
      if (category !== 'todas' && categorySlug(p.category) !== category) return false;
      if (query) {
        const haystack = `${p.name} ${p.description ?? ''}`.toLowerCase();
        if (!haystack.includes(query)) return false;
      }
      return true;
    });
  });

  protected readonly resultCount = computed(() => this.filteredProducts().length);

  public ngOnInit(): void {
    const slug = categorySlug(this.route.snapshot.queryParamMap.get('categoria'));
    if (slug) {
      this.categoryFilter.set(slug);
    }
    this.load();
  }

  /** Pide los productos a la API y actualiza los signals según el resultado. */
  protected load(): void {
    this.loadingProducts.set(true);
    this.errorProducts.set('');
    this.apiService.getProducts().subscribe({
      next: (data) => {
        this.products.set(this.toList(data));
        this.loadingProducts.set(false);
      },
      error: (err) => {
        this.errorProducts.set(this.readError(err));
        this.loadingProducts.set(false);
      },
    });
  }

  protected retry(): void {
    this.load();
  }

  protected onSearchInput(event: Event): void {
    this.search.set((event.target as HTMLInputElement).value);
  }

  protected onCategoryChange(event: Event): void {
    this.categoryFilter.set((event.target as HTMLSelectElement).value);
  }

  protected toggleAvailable(): void {
    this.onlyAvailable.update((value) => !value);
  }

  /** Abre/cierra el menú contextual de administrador. */
  protected toggleAdminMenu(): void {
    this.adminMenuOpen.update((open) => !open);
  }

  /** Cierra el menú de administrador (al hacer clic fuera). */
  protected closeAdminMenu(): void {
    this.adminMenuOpen.set(false);
  }

  protected clearFilters(): void {
    this.search.set('');
    this.categoryFilter.set('todas');
    this.onlyAvailable.set(false);
  }

  /** Formatea un precio en pesos chilenos (CLP), sin decimales. */
  protected price(value: number | null | undefined): string {
    const amount = typeof value === 'number' ? value : Number(value ?? 0);
    const safe = Number.isFinite(amount) ? amount : 0;
    return new Intl.NumberFormat('es-CL', {
      style: 'currency',
      currency: 'CLP',
      maximumFractionDigits: 0,
    }).format(safe);
  }

  /** Clasifica el stock para pintar el indicador (verde/ámbar/rojo). */
  protected stockLevel(stock: number | null | undefined): StockLevel {
    const value = typeof stock === 'number' ? stock : Number(stock ?? 0);
    if (!Number.isFinite(value) || value <= 0) return 'out';
    if (value <= 10) return 'low';
    return 'ok';
  }

  /**
   * Normaliza la respuesta de la API a un array de productos.
   * Soporta array directo y objetos con `products` o `content` (paginado).
   */
  private toList(data: unknown): Product[] {
    if (Array.isArray(data)) return data as Product[];
    const obj = data as { products?: Product[]; content?: Product[] } | null;
    if (obj && Array.isArray(obj.products)) return obj.products;
    if (obj && Array.isArray(obj.content)) return obj.content;
    return [];
  }

  /** Convierte un error HTTP/JS en un texto corto para mostrar al usuario. */
  private readError(err: unknown): string {
    const e = err as { status?: number; message?: string } | null;
    return e?.status ? `HTTP ${e.status}` : (e?.message ?? 'Error desconocido');
  }
}
