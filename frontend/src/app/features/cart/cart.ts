import { Component, effect, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { ApiService } from '../../core/api/api.service';
import { CartService } from '../../core/cart/cart.service';
import type { DispatchType, Order } from '../../core/api/models/order.model';
import { categoryIcon } from '../../core/catalog/categories';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './cart.html',
  styleUrl: './cart.css',
})
export class CartComponent {
  protected authService = inject(AuthService);
  protected apiService = inject(ApiService);
  protected cartService = inject(CartService);

  protected readonly iconFor = categoryIcon;

  // Datos del formulario de checkout
  protected customerName = signal('');
  protected dispatchType = signal<DispatchType>('DELIVERY');
  protected branchId = signal<number>(1);

  // Estados de envío y feedback
  protected submitting = signal(false);
  protected errorMessage = signal('');
  protected createdOrder = signal<Order | null>(null);

  constructor() {
    // Rellena automáticamente el nombre del cliente cuando carga la sesión de Entra ID
    effect(() => {
      const loggedName = this.authService.userName();
      if (loggedName && !this.customerName().trim()) {
        this.customerName.set(loggedName);
      }
    });
  }

  protected onCustomerNameInput(event: Event): void {
    this.customerName.set((event.target as HTMLInputElement).value);
  }

  protected setDispatchType(type: DispatchType): void {
    this.dispatchType.set(type);
  }

  protected onBranchChange(event: Event): void {
    const val = Number((event.target as HTMLSelectElement).value);
    if (Number.isFinite(val) && val > 0) {
      this.branchId.set(val);
    }
  }

  protected increase(productId: number): void {
    this.errorMessage.set('');
    this.cartService.increaseQuantity(productId);
  }

  protected decrease(productId: number): void {
    this.errorMessage.set('');
    this.cartService.decreaseQuantity(productId);
  }

  protected remove(productId: number): void {
    this.errorMessage.set('');
    this.cartService.removeProduct(productId);
  }

  protected clearCart(): void {
    if (window.confirm('¿Vaciar todos los productos del carrito?')) {
      this.cartService.clear();
      this.errorMessage.set('');
    }
  }

  protected submitOrder(event?: Event): void {
    event?.preventDefault();
    this.errorMessage.set('');

    if (!this.authService.authenticatedUser()) {
      this.errorMessage.set('Debes iniciar sesión con tu cuenta antes de confirmar el pedido.');
      return;
    }

    const items = this.cartService.cartItems();
    if (items.length === 0) {
      this.errorMessage.set('Tu carrito está vacío.');
      return;
    }

    const name = this.customerName().trim();
    if (!name) {
      this.errorMessage.set('Ingresa un nombre para el pedido.');
      return;
    }

    this.submitting.set(true);

    this.apiService
      .createOrder({
        customerName: name,
        branchId: this.branchId(),
        dispatchType: this.dispatchType(),
        items: items.map((item) => ({
          productId: item.product.id,
          productName: item.product.name,
          quantity: item.quantity,
          unitPrice: item.product.price,
        })),
      })
      .subscribe({
        next: (order) => {
          this.submitting.set(false);
          this.createdOrder.set(order);
          this.cartService.clear();
        },
        error: (err) => {
          this.submitting.set(false);
          this.errorMessage.set(this.readError(err));
        },
      });
  }

  protected startNewOrder(): void {
    this.createdOrder.set(null);
    this.errorMessage.set('');
  }

  /** Formatea un precio en pesos chilenos (CLP), igual que en MenuComponent. */
  protected price(value: number | null | undefined): string {
    const amount = typeof value === 'number' ? value : Number(value ?? 0);
    const safe = Number.isFinite(amount) ? amount : 0;
    return new Intl.NumberFormat('es-CL', {
      style: 'currency',
      currency: 'CLP',
      maximumFractionDigits: 0,
    }).format(safe);
  }

  private readError(err: unknown): string {
    const e = err as { status?: number; error?: { message?: string }; message?: string } | null;
    if (e?.status === 401) return 'Tu sesión expiró o no está autenticada (401). Vuelve a iniciar sesión.';
    if (e?.status === 400) return e?.error?.message || 'Datos inválidos o stock insuficiente (400).';
    if (e?.status === 409) return e?.error?.message || 'Conflicto de stock al procesar el pedido (409).';
    return e?.error?.message ?? (e?.status ? `Error del servidor (HTTP ${e.status})` : 'No se pudo procesar el pedido.');
  }
}