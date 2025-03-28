# Use official OpenJDK runtime as a parent image
FROM eclipse-temurin:17-jdk-jammy as builder

# Set the working directory in the container
WORKDIR /app

# Copy the Gradle or Maven files to leverage Docker cache
COPY build.gradle.kts settings.gradle.kts gradlew ./
COPY gradle ./gradle

# Download dependencies (this layer will be cached unless build files change)
# The || return 0 is there because some Gradle projects might fail when running dependencies without all source code,
# we want the build to continue anyway (the important part is downloading dependencies),
# the actual build will happen later with all source code present
RUN ./gradlew --info --console=plain dependencies || return 0

# Copy the rest of the source code
COPY src ./src

# Build the application (creates a fat JAR by default with Spring Boot Gradle plugin)
RUN ./gradlew --info --console=plain clean build

# Use a smaller JRE runtime image for the final image
FROM eclipse-temurin:17-jre-jammy

# Set the working directory
WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

COPY docker/application.yml ./config/

# Expose the port the app runs on
EXPOSE 8080

# Set environment variables (customize as needed)
ENV SPRING_PROFILES_ACTIVE=prod,keycloak
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Set the entry point to run the application
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]