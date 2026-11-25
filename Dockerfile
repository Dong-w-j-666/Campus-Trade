FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY server/pom.xml ./
RUN mvn -q -DskipTests dependency:go-offline
COPY server/src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /build/target/campus-trade-server-0.1.0.jar app.jar
RUN mkdir -p /app/uploads
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
