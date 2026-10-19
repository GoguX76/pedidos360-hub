# Docker — Contenedores

## Dockerfiles

Cada microservicio tiene un Dockerfile multi-etapa:

```dockerfile
# Etapa 1: build con Gradle + JDK 25
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew --version
COPY src ./src
RUN ./gradlew bootJar --no-daemon

# Etapa 2: runtime con JRE 25
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/build/libs/<servicio>-0.0.1-SNAPSHOT.jar app.jar
EXPOSE <puerto>
HEALTHCHECK --interval=30s --timeout=5s --retries=3 CMD curl -f http://localhost:<puerto>/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## Docker Compose — RabbitMQ

`infra/rabbitmq/docker-compose.yml` levanta:

| Servicio | Imagen | Puertos |
|---|---|---|
| `rabbit1` | rabbitmq:4-management | 5672, 15672 |
| `rabbit2` | rabbitmq:4-management | 5673 |
| `cluster-init` | rabbitmq:4-management | — |

El servicio `cluster-init` espera a que ambos nodos estén sanos, verifica si `rabbit2` ya pertenece al cluster, y si no lo está:

```
stop_app → reset → join_cluster rabbit@rabbit1 → start_app
```

## Healthchecks

- Backend: `curl http://localhost:<puerto>/actuator/health`
- RabbitMQ: `rabbitmq-diagnostics -q ping`

## Red

Los contenedores de RabbitMQ usan una red bridge `rabbitnet` para comunicarse entre sí.
