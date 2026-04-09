FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY target/auth-server-1.0.0.jar app.jar
EXPOSE 9000
ENTRYPOINT ["java", "-jar", "app.jar"]
