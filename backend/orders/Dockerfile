# Imagen de ORDERS.
#
# NOTA IMPORTANTE sobre el contexto: el proyecto usa un composite build de
# Gradle (ver settings.gradle -> includeBuild('../common-messaging')), por lo
# que este Dockerfile se construye con el contexto en backend/:
#
#   docker build -f orders/Dockerfile -t orders:latest backend/
#
# En las EC2 de despliegue NO se usa este archivo: alli vive un Dockerfile
# "runtime-only" (solo copia el JAR) que los scripts deploy-ec2.sh NO pisan.

# Etapa 1: build con Gradle + JDK 25
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY common-messaging/settings.gradle common-messaging/build.gradle ./common-messaging/
COPY common-messaging/src ./common-messaging/src
COPY orders/gradle ./orders/gradle
COPY orders/gradlew orders/settings.gradle orders/build.gradle ./orders/
COPY orders/src ./orders/src
WORKDIR /app/orders
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew bootJar --no-daemon

# Etapa 2: runtime con JRE 25
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/orders/build/libs/orders-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=5s --retries=3 CMD curl -f http://localhost:8081/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
