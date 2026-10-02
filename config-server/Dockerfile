FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY . .

RUN mvn -pl config-server -am clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /workspace/config-server/target/config-server-0.0.1-SNAPSHOT.jar app.jar
COPY config-repo /config-repo

ENV CONFIG_REPO_LOCATION=file:/config-repo

EXPOSE 8888

ENTRYPOINT ["java", "-jar", "/app/app.jar"]