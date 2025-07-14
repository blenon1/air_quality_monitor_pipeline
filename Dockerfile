FROM openjdk:11-jre-slim

# Installation des utilitaires
RUN apt-get update && \
    apt-get install -y curl procps && \
    rm -rf /var/lib/apt/lists/*

# Création utilisateur non-root
RUN groupadd -r appuser && \
    useradd -r -g appuser -d /app -s /bin/bash appuser

WORKDIR /app
RUN mkdir -p /app/logs /app/data /app/config

# Copier le JAR pré-compilé
COPY target/scala-2.13/air-quality-monitor-assembly-*.jar /app/app.jar

# Configuration
COPY application.conf.example /app/config/application.conf.example

# Scripts
COPY docker/entrypoint.sh /app/entrypoint.sh
COPY docker/healthcheck.sh /app/healthcheck.sh

RUN chmod +x /app/entrypoint.sh /app/healthcheck.sh
RUN chown -R appuser:appuser /app

USER appuser

ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC"
ENV APP_CONFIG="/app/config/application.conf"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD /app/healthcheck.sh

ENTRYPOINT ["/app/entrypoint.sh"]
CMD ["java", "-jar", "/app/app.jar"]