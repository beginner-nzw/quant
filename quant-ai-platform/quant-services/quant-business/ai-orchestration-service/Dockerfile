FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY . .
RUN mvn -pl quant-business/ai-orchestration-service -am clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/quant-business/ai-orchestration-service/target/ai-orchestration-service.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]