#!/bin/bash
set -e

echo "=== Démarrage Air Quality Monitor ==="

mkdir -p /app/logs /app/data

CONFIG_FILE="/app/config/application.conf"

if [ ! -f "$CONFIG_FILE" ]; then
    echo "Configuration par défaut"
    if [ -f "/app/config/application.conf.example" ]; then
        cp /app/config/application.conf.example "$CONFIG_FILE"
    else
        echo "demo = true" > "$CONFIG_FILE"
    fi
fi

JAVA_OPTS="${JAVA_OPTS} -Djava.awt.headless=true"

echo "Démarrage de l'application..."

if [ "$1" = "java" ]; then
    exec java $JAVA_OPTS -jar /app/app.jar
else
    exec "$@"
fi
EOF

# Recréer le script healthcheck
cat > docker/healthcheck.sh << 'EOF'
#!/bin/bash

if pgrep -f "java.*app.jar" >/dev/null; then
    echo "OK"
    exit 0
else
    echo "KO"
    exit 1
fi