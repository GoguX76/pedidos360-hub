/**
 * Producto del catálogo tal como lo expone `GET /api/v1/products`.
 * Los campos opcionales se marcan como tales porque el backend puede
 * omitirlos según el DTO.
 */
export interface Product {
  id: number;
  name: string;
  description?: string | null;
  price: number;
  stock: number;
  category?: string | null;
  available: boolean;
}

/**
 * Datos para crear o actualizar un producto.
 * Coincide con el `ProductRequest` del backend (`POST/PUT /api/v1/products`).
 */
export interface ProductPayload {
  name: string;
  description?: string | null;
  price: number;
  stock: number;
  category: string;
  available: boolean;
}
