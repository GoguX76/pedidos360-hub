# kafka-admin

Microservicio Spring Boot que administra Kafka (equivalente a rabbitmq-admin).
Todos los endpoints exigen un JWT del IDaaS (salvo /actuator/health).

| Metodo | Ruta | Que hace |
|---|---|---|
| GET | /api/kafka/topics | Lista topicos |
| GET | /api/kafka/topics/{name} | Particiones, lider, replicas, ISR y configuracion |
| POST | /api/kafka/topics | Crea topico (nombre, particiones, replicas, configs) |
| PUT | /api/kafka/topics/{name} | Sube particiones y/o cambia configs |
| DELETE | /api/kafka/topics/{name} | Elimina topico |
| GET | /api/kafka/topics/{name}/config | Lee configuracion |
| GET | /api/kafka/consumer-groups | Lista grupos |
| GET | /api/kafka/consumer-groups/{id} | Miembros y particiones asignadas |
| GET | /api/kafka/consumer-groups/{id}/offsets | Offsets y lag |

Ejemplos de cuerpo:

    POST /api/kafka/topics
    {"name":"pedidos.eventos","partitions":3,"replicationFactor":2,
     "configs":{"retention.ms":"604800000","cleanup.policy":"delete"}}

    PUT /api/kafka/topics/pedidos.eventos
    {"partitions":6,"configs":{"retention.ms":"259200000"}}

Configs permitidas: retention.ms, retention.bytes, cleanup.policy,
min.insync.replicas, segment.bytes, max.message.bytes, compression.type.
Kafka no permite reducir particiones.
Al arrancar crea los topicos de application.yml (kafka-admin.topics).

## Despliegue en EC2

La instancia actual `98.95.43.84` ejecuta Kafka y `kafka-admin` en contenedores
separados. Docker Compose publica los brokers en `9092` y `9093` y los anuncia
con la IP privada de la instancia. `kafka-admin` usa `kafka1:19092,kafka2:19092`
en la red Docker `kafkanet`; `orders` y `products` usan la IP privada y los
puertos publicados.

Kafka UI escucha solo en `127.0.0.1:8090` en la EC2. Para abrirla desde el
equipo local, crea un túnel SSH:

```bash
ssh -i C:/Users/pv-alumno/Downloads/pedidos360.pem \
  -L 8090:127.0.0.1:8090 ec2-user@98.95.43.84
```

Luego visita `http://localhost:8090`. No se requiere abrir 8090 en el Security
Group.

En la EC2, los brokers y Kafka UI se levantan desde `/home/ec2-user/kafka`:

```bash
cd /home/ec2-user/kafka
docker compose up -d zookeeper kafka1 kafka2 kafka-ui
```

Desde `backend/kafka-admin`:

```bash
KEY_PATH=C:/Users/pv-alumno/Downloads/pedidos360.pem \
KAFKA_BOOTSTRAP_SERVERS=kafka1:19092,kafka2:19092 \
./deploy-ec2.sh
```

## Security Group

Para la instancia Kafka, usa un grupo propio con estas reglas:

| Dirección | Protocolo | Puerto | Origen |
|---|---|---|---|
| Inbound | TCP | 8084 | `0.0.0.0/0` si API Gateway integra la IP pública (API requiere JWT) |
| Inbound | TCP | 22 | Solo tu IP administrativa |
| Inbound | TCP | 9092-9093 | SG compartido de `orders` y `products` |
| Outbound | TCP | 443 | `0.0.0.0/0` |
