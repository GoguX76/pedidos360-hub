FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY common-messaging/settings.gradle common-messaging/build.gradle ./common-messaging/
COPY common-messaging/src ./common-messaging/src
COPY kafka-streams/gradle ./kafka-streams/gradle
COPY kafka-streams/gradlew kafka-streams/settings.gradle kafka-streams/build.gradle ./kafka-streams/
COPY kafka-streams/src ./kafka-streams/src
WORKDIR /workspace/kafka-streams
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew bootJar --no-daemon

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /workspace/kafka-streams/build/libs/kafka-streams-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
