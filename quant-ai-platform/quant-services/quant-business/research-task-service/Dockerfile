FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY . .
RUN mvn -pl quant-business/research-task-service -am clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/quant-business/research-task-service/target/research-task-service.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]