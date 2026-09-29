# FROM gradle:8.14-jdk21 AS build

# WORKDIR /app

# COPY gradle ./gradle
# COPY gradlew ./gradlew
# COPY gradlew.bat ./gradlew.bat
# COPY settings.gradle ./settings.gradle
# COPY model ./model
# COPY messagingservice/ ./messagingservice

# WORKDIR /app/messagingservice
# RUN gradle bootJar --no-daemon

# FROM gcr.io/distroless/java21-debian12

# COPY --from=build /app/messagingservice/build/libs/*.jar messagingservice.jar
# EXPOSE 8080
# CMD ["messagingservice.jar"]

FROM gradle:8.8-jdk21 AS build
WORKDIR /workspace
COPY . .
RUN ./gradlew :messagingservice:bootJar --no-daemon && \
    JAR="$(find messagingservice/build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' -print -quit)" && \
    test -n "$JAR" && cp "$JAR" /tmp/app.jar

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/* && useradd --system --uid 10001 app
COPY --from=build --chown=10001:10001 /tmp/app.jar /app/app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
