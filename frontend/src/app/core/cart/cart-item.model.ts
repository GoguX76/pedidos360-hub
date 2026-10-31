import type { Product } from '../api/models/product.model';

/**
 * Producto y cantidad almacenados en el carrito
 */

export interface CartItem {
    product: Product;
    quantity: number;
}