# API — Pedidos 360 Hub

Base local: `http://localhost:8081` (orders), `http://localhost:8082` (products).
En despliegue van tras el gateway: `https://y2fmjg2ed2.execute-api.us-east-1.amazonaws.com`.
Todos los endpoints (salvo `/actuator/health` y `/actuator/info`) exigen JWT válido.

## Orders — `/api/v1/orders`

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/{id}` | Pedido por ID |
| `GET` | `/customer/{customerId}` | Pedidos de un cliente |
| `GET` | `/branch/{branchId}` | Pedidos de un local |
| `GET` | `/status/{status}` | Pedidos por estado |
| `POST` | `/` | Crear pedido (201). El `customerId` se toma del claim `oid` del JWT, no del body |
| `PATCH` | `/{id}/status` | Avanzar estado según la máquina de estados |
| `DELETE` | `/{id}` | Cancelar pedido (solo si está en `CREATED`) |

### Crear pedido — body

```json
{
  "customerName": "Juana Pérez",
  "branchId": 1,
  "dispatchType": "DELIVERY",
  "items": [
    { "productName": "Pizza familiar", "quantity": 2, "unitPrice": 12990.00 }
  ]
}
```

* `dispatchType`: `DELIVERY` o `PICKUP` (insensible a mayúsculas).
* `items`: mínimo 1; `quantity` y `unitPrice` validados.
* Respuesta: `id`, `customerId`, `customerName`, `branchId`, `status`,
  `dispatchType`, `totalAmount` (calculado en servidor), `createdAt`,
  `updatedAt`, `items`.

### Máquina de estados (`OrderStatus`)

```
CREATED → IN_PREPARATION → READY → DISPATCHED → DELIVERED
   │            │
   └→ CANCELLED ┘
```

* `DELIVERED` y `CANCELLED` son finales (sin transiciones).
* Toda transición fuera de este grafo responde `400`.
* `DELETE` solo cancela desde `CREATED`; para el resto, usar `PATCH /status`.

### Errores

Manejados por `GlobalExceptionHandler`: `404` si el pedido no existe,
`400` por validaciones, tipo de despacho/estado inválido o transición ilegal.

## Products — `/api/v1/products`

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/` | Listar (`?onlyAvailable=true` filtra disponibles) |
| `GET` | `/{id}` | Producto por ID |
| `POST` | `/` | Crear (201) |
| `PUT` | `/{id}` | Actualizar |
| `POST` | `/{id}/decrement?quantity=N` | Descontar stock (atómico) |
| `DELETE` | `/{id}` | Eliminar (204) |

### Producto — body

```json
{
  "name": "Bebida 500ml",
  "description": "Bebida fría",
  "price": 1500.00,
  "stock": 100,
  "category": "Bebidas",
  "available": true
}
```

Validaciones: `name`, `price` (> 0), `stock` (≥ 0) y `category` obligatorios.
