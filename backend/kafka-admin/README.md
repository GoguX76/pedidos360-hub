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
