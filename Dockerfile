# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Install curl for container health check, and add non-root user for security
RUN apk add --no-cache curl && addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY target/alumni-intelligence-api-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
