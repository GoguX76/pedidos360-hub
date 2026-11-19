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
| EC2 #3 | `frontend` | 8080 |
| EC2 #4 | RabbitMQ (Docker) | 5672, 15672 |
| EC2 #5 | Kafka + `kafka-admin` (Docker) | 9092, 9093, 8084 |

## Conexión entre instancias

Los Security Groups permiten tráfico entre las instancias:

```
EC2 #1 (orders)     → EC2 #4 (RabbitMQ):5672
EC2 #2 (products)   → EC2 #4 (RabbitMQ):5672
EC2 #1/#2 (orders/products) → EC2 #5 (Kafka):9092/9093
EC2 #5 (`kafka-admin`) → Kafka brokers por `kafkanet` (19092)
Frontend (navegador) → API Gateway → EC2 #1, EC2 #2
```

## Kafka Admin

La instancia `98.95.43.84` ejecuta Kafka y `kafka-admin`, y usa un Security
Group propio. Se permite TCP 8084 para la integración pública de API Gateway,
SSH 22 solo desde la IP administrativa y TCP 9092/9093 desde el SG compartido
por `orders` y `products`. Los endpoints de `kafka-admin` requieren JWT salvo
health; es preferible usar integración privada/VPC Link si se configura después.

## Despliegue

Cada microservicio tiene:
- `Dockerfile`: build multi-etapa (JDK 25 → JRE 25)
- `deploy-ec2.sh`: compila, sube JAR a EC2, reconstruye imagen y relanza contenedor

## API Gateway

- **URL**: `https://y2fmjg2ed2.execute-api.us-east-1.amazonaws.com`
- **Rutas**: `/api/v1/orders*` → EC2 #1, `/api/v1/products*` → EC2 #2
- **Método**: HTTP API con integración HTTP_PROXY
