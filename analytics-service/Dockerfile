FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --version

COPY common-events common-events
COPY analytics-service analytics-service
COPY url-service url-service
RUN ./gradlew :analytics-service:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/analytics-service/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
