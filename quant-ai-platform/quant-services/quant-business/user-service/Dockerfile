FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY . .
RUN mvn -pl quant-business/user-service -am clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /workspace/quant-business/user-service/target/user-service.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
