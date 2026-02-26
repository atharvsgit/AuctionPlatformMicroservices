FROM maven:3.9.4-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy the pom.xml and source code
COPY pom.xml .
COPY src ./src

# Package the application (skipping tests to speed up the build)
RUN mvn clean package -DskipTests

# Stage 2: Create the lightweight runtime image
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the built jar file from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]