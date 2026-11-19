# Imagen runtime-only de KAFKA-ADMIN.
#
# Construir desde backend/kafka-admin y generar primero el JAR:
#   .\gradlew.bat bootJar
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY build/libs/kafka-admin-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
