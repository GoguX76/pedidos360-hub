# AWS — Infraestructura

## Componentes en AWS

| Componente | Descripción |
|---|---|
| EC2 | Máquinas virtuales donde corren los microservicios y RabbitMQ |
| API Gateway | Punto de entrada HTTP para el frontend (`https://y2fmjg2ed2.execute-api.us-east-1.amazonaws.com`) |
| Security Groups | Reglas de firewall entre instancias |

## Instancias EC2

| Instancia | Contenido | Puerto |
|---|---|---|
| EC2 #1 | `orders` | 8081 |
| EC2 #2 | `products` | 8082 |
| EC2 #3 | `frontend` (Nginx) | 80, 443 |
| EC2 #4 | RabbitMQ (Docker) | 5672, 15672 |

## Conexión entre instancias

Los Security Groups permiten tráfico entre las instancias:

```
EC2 #1 (orders)     → EC2 #4 (RabbitMQ):5672
EC2 #2 (products)   → EC2 #4 (RabbitMQ):5672
Frontend (navegador) → API Gateway → EC2 #1, EC2 #2
```

## Despliegue

Cada microservicio tiene:
- `Dockerfile`: build multi-etapa (JDK 25 → JRE 25)
- `deploy-ec2.sh`: compila, sube JAR a EC2, reconstruye imagen y relanza contenedor

El frontend se compila en GitHub Actions y se publica en `/usr/share/nginx/html`
en la EC2 #3; Nginx sirve los archivos estáticos en los puertos 80 y 443.

## API Gateway

- **URL**: `https://y2fmjg2ed2.execute-api.us-east-1.amazonaws.com`
- **Rutas**: `/api/v1/orders*` → EC2 #1, `/api/v1/products*` → EC2 #2
- **Método**: HTTP API con integración HTTP_PROXY
