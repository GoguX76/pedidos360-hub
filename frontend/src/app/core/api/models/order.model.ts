/**
 * Restringe el tipo de despacho a los valores aceptados
 */

export type DispatchType = 'DELIVERY' | 'PICKUP';

export type OrderStatus =
  | 'CREATED'
  | 'IN_PREPARATION'
  | 'READY'
  | 'DISPATCHED'
  | 'DELIVERED'
  | 'CANCELLED';

/**
 * Datos de un producto incluidos al crear un pedido
 */
export interface OrderItemPayload {
  productId: number;
  productName: string;
  quantity: number;
  unitPrice: number;
}

export interface OrderPayload {
  customerName: string;
  branchId: number;
  dispatchType: DispatchType;
  items: OrderItemPayload[];
}

export interface OrderItem {
  id: number;
  productName: string;
  quantity: number;
  unitPrice: number;
}

export interface Order {
  id: number;
  customerId: string;
  customerName: string;
  branchId: number;
  status: OrderStatus;
  dispatchType: DispatchType;
  totalAmount: number;
  createdAt: string;
  updatedAt: string;
  items: OrderItem[];
}
