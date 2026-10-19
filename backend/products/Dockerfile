# Imagen de PRODUCTS.
#
# NOTA IMPORTANTE sobre el contexto: el proyecto usa un composite build de
# Gradle (ver settings.gradle -> includeBuild('../common-messaging')), por lo
# que este Dockerfile se construye con el contexto en backend/:
#
#   docker build -f products/Dockerfile -t products:latest backend/
#
# En las EC2 de despliegue NO se usa este archivo: alli vive un Dockerfile
# "runtime-only" (solo copia el JAR) que los scripts deploy-ec2.sh NO pisan.

# Etapa 1: build con Gradle + JDK 25
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY common-messaging/settings.gradle common-messaging/build.gradle ./common-messaging/
COPY common-messaging/src ./common-messaging/src
COPY products/gradle ./products/gradle
COPY products/gradlew products/settings.gradle products/build.gradle ./products/
COPY products/src ./products/src
WORKDIR /app/products
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew bootJar --no-daemon

# Etapa 2: runtime con JRE 25
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/products/build/libs/products-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8082
HEALTHCHECK --interval=30s --timeout=5s --retries=3 CMD curl -f http://localhost:8082/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
