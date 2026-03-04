## 🐳 Dockerfile

# Use OpenJDK 21 base image
FROM eclipse-temurin:21-jdk-alpine

# Set work directory
WORKDIR /app

# Copy the packaged jar file
COPY target/my-market-app-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java","-jar","app.jar"]