FROM openjdk:23-jdk-slim

WORKDIR /app

COPY out/artifacts/Server_jar/Server.jar app.jar

EXPOSE 3000

ENTRYPOINT ["java", "-jar", "app.jar"]