#!/usr/bin/env bash
# Despliegue de KAFKA-ADMIN en su EC2 individual.
set -euo pipefail

KEY_PATH="${KEY_PATH:?Exporta KEY_PATH con la ruta de tu llave .pem}"
KAFKA_ADMIN_IP="${KAFKA_ADMIN_IP:-98.95.43.84}"
REMOTE_DIR="/home/ec2-user/kafka-admin"

KAFKA_BOOTSTRAP_SERVERS="${KAFKA_BOOTSTRAP_SERVERS:-kafka1:19092,kafka2:19092}"
AZURE_TENANT_ID="${AZURE_TENANT_ID:-6a3978a5-1a22-4be4-bbb8-a7c6279c471e}"
AZURE_API_AUDIENCE="${AZURE_API_AUDIENCE:-api://4d1afbc7-9d81-4ef4-a0e2-fd0ec724f8f4}"
CORS_ALLOWED_ORIGINS="${CORS_ALLOWED_ORIGINS:-http://localhost:4200,https://34.230.203.32}"

./gradlew bootJar --no-daemon
JAR="$(ls -t build/libs/kafka-admin-*.jar | head -1)"
echo "JAR a desplegar: $JAR"

ssh -i "$KEY_PATH" "ec2-user@${KAFKA_ADMIN_IP}" \
  "mkdir -p '${REMOTE_DIR}/build/libs'"
scp -i "$KEY_PATH" "$JAR" "ec2-user@${KAFKA_ADMIN_IP}:${REMOTE_DIR}/build/libs/"
scp -i "$KEY_PATH" Dockerfile "ec2-user@${KAFKA_ADMIN_IP}:${REMOTE_DIR}/"

ssh -i "$KEY_PATH" "ec2-user@${KAFKA_ADMIN_IP}" \
  "KAFKA_BOOTSTRAP_SERVERS='${KAFKA_BOOTSTRAP_SERVERS}' AZURE_TENANT_ID='${AZURE_TENANT_ID}' AZURE_API_AUDIENCE='${AZURE_API_AUDIENCE}' CORS_ALLOWED_ORIGINS='${CORS_ALLOWED_ORIGINS}' bash -s" <<'EOF'
set -e
cd /home/ec2-user/kafka-admin
docker network inspect kafkanet >/dev/null 2>&1 || {
  echo "ERROR: la red Docker 'kafkanet' no existe. Inicia Kafka antes de kafka-admin."
  exit 1
}
docker build -t kafka-admin:latest .
docker rm -f kafka-admin || true
docker run -d --name kafka-admin --restart unless-stopped --memory=768m --network kafkanet \
  -p 8084:8084 \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS="$KAFKA_BOOTSTRAP_SERVERS" \
  -e AZURE_TENANT_ID="$AZURE_TENANT_ID" \
  -e AZURE_API_AUDIENCE="$AZURE_API_AUDIENCE" \
  -e CORS_ALLOWED_ORIGINS="$CORS_ALLOWED_ORIGINS" \
  -e JAVA_TOOL_OPTIONS="-Xms128M -Xmx512M" \
  kafka-admin:latest
echo "--- contenedores ---"
docker ps --format '{{.Names}} {{.Status}}'
echo "--- health (hasta 60s) ---"
for i in $(seq 1 12); do
  if curl -sf http://localhost:8084/actuator/health > /dev/null; then
    echo "kafka-admin UP"
    exit 0
  fi
  sleep 5
done
echo "kafka-admin NO respondio health a tiempo (revisar: docker logs kafka-admin)"
exit 1
EOF
