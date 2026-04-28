FROM maven:3.9.11-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml ./
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn clean -q -DskipTests dependency:go-offline && mvn -q -DskipTests package

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN addgroup -S rideflow && adduser -S rideflow -G rideflow

RUN mkdir -p /app/data && chown -R rideflow:rideflow /app

COPY --from=build /app/target/*.war /app/app.war

RUN chown -R rideflow:rideflow /app

USER rideflow

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.war"]