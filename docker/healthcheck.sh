# Healthcheck simplifié
#!/bin/bash

# Test simple : vérifier que le processus Java tourne
if pgrep -f "java.*app.jar" >/dev/null; then
    echo "Application running"
    exit 0
else
    echo "Application not running"
    exit 1
fi