# AWS — Infraestructura

## Componentes en AWS

| Componente | Descripción |
|---|---|
| EC2 | Máquinas virtuales donde corren los microservicios y RabbitMQ |
| API Gateway | Punto de entrada HTTP para el frontend (URL configurada por entorno) |
| Security Groups | Reglas de firewall entre instancias |

## Instancias EC2

| Instancia | Contenido | Puerto |
|---|---|---|
| EC2 #1 | `orders` | 8081 |
| EC2 #2 | `products` | 8082 |
| EC2 #3 | `frontend` (Nginx) | 80, 443 |
| EC2 #4 | RabbitMQ (Docker) | 5672, 15672 |
| EC2 #5 | Kafka (3 brokers) + `kafka-admin` (Docker) | 9092-9094, 8084 |

## Conexión entre instancias

Los Security Groups permiten tráfico entre las instancias:

```
EC2 #1 (orders)     → EC2 #4 (RabbitMQ):5672
EC2 #2 (products)   → EC2 #4 (RabbitMQ):5672
EC2 #1/#2 (orders/products) → EC2 #5 (Kafka):9092/9093/9094
EC2 #5 (`kafka-admin`) → Kafka brokers por `kafkanet` (19092)
Frontend (navegador) → API Gateway → EC2 #1, EC2 #2
```

## Kafka Admin

La instancia EC2 dedicada ejecuta Kafka y `kafka-admin`, y usa un Security
Group propio. Se permite TCP 8084 para la integración pública de API Gateway,
SSH 22 solo desde la IP administrativa y TCP 9092-9094 desde el SG compartido
por `orders` y `products`. Los endpoints de `kafka-admin` requieren JWT salvo
health; es preferible usar integración privada/VPC Link si se configura después.

## Despliegue

Cada microservicio tiene:
- `Dockerfile`: build multi-etapa (JDK 25 → JRE 25)
- `deploy-ec2.sh`: compila, sube JAR a EC2, reconstruye imagen y relanza contenedor

Los workflows de `orders` y `products` requieren el secreto de GitHub Actions
`KAFKA_BOOTSTRAP_SERVERS`, con los brokers alcanzables desde esas EC2 en formato
separado por comas (`host1:9092,host2:9093,host3:9094`). Usa las direcciones
privadas de los brokers dentro de la VPC; no guardes esas direcciones en el repo.

El frontend se compila en GitHub Actions y se publica en `/usr/share/nginx/html`
en la EC2 #3; Nginx sirve los archivos estáticos en los puertos 80 y 443.

## API Gateway

- **URL**: configurada por entorno (no versionar el endpoint real)
- **Rutas**: `/api/v1/orders*` → EC2 #1, `/api/v1/products*` → EC2 #2
- **Método**: HTTP API con integración HTTP_PROXY
