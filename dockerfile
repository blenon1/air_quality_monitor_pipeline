FROM openjdk:11-jre-slim

RUN apt-get update && \
    apt-get install -y curl && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY target/scala-2.13/air-quality-monitor-assembly-*.jar app.jar


RUN mkdir -p /app/logs /app/data

RUN groupadd -r appuser && useradd -r -g appuser appuser
RUN chown -R appuser:appuser /app
USER appuser


EXPOSE 8080


HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/health || exit 1

CMD ["java", "-jar", "app.jar"]