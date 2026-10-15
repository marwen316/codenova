FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package -DskipTests
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -g 10001 -S codenova && adduser -u 10001 -S codenova -G codenova \
    && mkdir -p /app/media && chown codenova:codenova /app/media
USER codenova
COPY --from=build /app/target/codenova-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
