import { Injectable, computed, signal } from '@angular/core';
import type { Product } from '../api/models/product.model';
import type { CartItem } from './cart-item.model';

@Injectable({
    providedIn: 'root',
})
export class CartService {
    /**
     * Guarda el estado interno del carrito
     */
    private readonly items = signal<CartItem[]>([]);

    /**
     * Expone el carrito para lectura, sin permitir modificaciones directas
     */
    public readonly cartItems = this.items.asReadonly();

    /**
     * Calcula la cantidad total de productos en el carrito
     */
    public readonly totalItems = computed(() =>
        this.items().reduce((total, item) => total + item.quantity, 0),
    );

    /**
     * Calcula el importe total del carrito
     */
    public readonly totalAmount = computed(() =>
        this.items().reduce(
            (total, item) => total + item.product.price * item.quantity,
            0,
        ),
    );

    /**
     * Agrega un producto al carrito o aumenta su cantidad
     * @param product Producto que se desea agregar
     * @returns true si se agregó; false si no hay stock disponible
     */
    public addProduct(product: Product): boolean {
        if (!product.available || product.stock <= 0) {
            return false;
        }

        const existingItem = this.items().find(
            (item) => item.product.id === product.id,
        );

        if (existingItem && existingItem.quantity >= product.stock) {
            return false;
        }

        if (existingItem) {
            this.items.update((items) =>
                items.map((item) =>
                    item.product.id === product.id
                    ? { ...item, quantity: item.quantity + 1 }
                    : item,
                ),
            );
            return true;
        }

        this.items.update((items) => [
            ...items,
            {
                product,
                quantity: 1,
            },
        ]);
        return true;
    }

    /**
     * Aumenta en una unidad la cantidad de un producto en el carrito
     * @param productId Identificador único del producto
     * @returns true si se aumentó la cantidad, false si no existe, no está disponible o alcanzó el stock
     */
    public increaseQuantity(productId: number): boolean {
        const existingItem = this.items().find(
            (item) => item.product.id === productId,
        );

        if (!existingItem) {
            return false;
        }

        if (!existingItem.product.available || existingItem.quantity >= existingItem.product.stock) {
            return false;
        }

        this.items.update((items) =>
            items.map((item) =>
                item.product.id === productId
                ? { ...item, quantity: item.quantity + 1 }
                : item,
            ),
        );
        return true;
    }

    /**
     * Disminuye en una unidad la cantidad de un producto en el carrito
     *
     * Si la cantidad llega a uno, elimina el producto
     *
     * @param productId Identificador único del producto
     * @returns true si disminuye la cantidad, false si el producto no existe
     */
    public decreaseQuantity(productId: number): boolean {
        const existingItem = this.items().find(
            (item) => item.product.id === productId,
        );

        if (!existingItem) {
            return false;
        }

        if (existingItem.quantity > 1) {
            this.items.update((items) =>
                items.map((item) =>
                    item.product.id === productId
                    ? { ...item, quantity: item.quantity - 1 }
                    : item,
                ),
            );

            return true;
        }

        return this.removeProduct(productId);
    }

    /**
     * Elimina completamente un producto del carrito
     *
     * @param productId Identificador único del producto
     * @returns true si se eliminó; false si no existía
     */
    public removeProduct(productId: number): boolean {
        const existingItem = this.items().some(
            (item) => item.product.id === productId,
        );

        if (!existingItem) {
            return false;
        }

        this.items.update((items) =>
            items.filter((item) => item.product.id !== productId),
        );

        return true;
    }

    /**
     * Vacía completamente el carrito
     */
    public clear(): void {
        this.items.set([]);
    }
}