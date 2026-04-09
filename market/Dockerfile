FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY target/market-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
