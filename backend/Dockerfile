FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace
COPY . /workspace
RUN mvn -DskipTests -q package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user for security
RUN addgroup -S app && adduser -S app -G app

COPY --from=builder /workspace/target/*.jar /app/app.jar
RUN chown -R app:app /app

USER app

EXPOSE 8080

# JVM optimizations for container environment
ENTRYPOINT ["java", \
  "-Xmx512m", \
  "-XX:+UseG1GC", \
  "-XX:+UseStringDeduplication", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Dfile.encoding=UTF-8", \
  "-jar", \
  "/app/app.jar"]

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/stocks || exit 1
