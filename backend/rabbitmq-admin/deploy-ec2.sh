#!/usr/bin/env bash
# Deploy del microservicio RABBITMQ-ADMIN a su EC2.
#
# Uso (Git Bash en Windows, desde backend/rabbitmq-admin):
#   ./deploy-ec2.sh
#
# Que hace:
#   1. Compila el JAR localmente (./gradlew bootJar).
#   2. Sube el JAR a la EC2 (el Dockerfile runtime-only ya vive en
#      /home/ec2-user/Dockerfile y NO se pisa).
#   3. Reconstruye la imagen y relanza el contenedor con las variables
#      de entorno de produccion.
#
# IMPORTANTE: sin RABBITMQ_HOST el contenedor no puede conectarse al
# broker. No omitir las -e.
set -euo pipefail

# --- Configuracion (sobrescribible por entorno) ---
KEY_PATH="${KEY_PATH:-C:/Users/Scor/Downloads/pedidos360.pem}"
ADMIN_IP="${ADMIN_IP:-}"
REMOTE_DIR="/home/ec2-user"
RABBITMQ_HOST="${RABBITMQ_HOST:-localhost}"
RABBITMQ_PORT="${RABBITMQ_PORT:-5672}"
RABBITMQ_USER="${RABBITMQ_USER:-pedidos}"
RABBITMQ_PASS="${RABBITMQ_PASS:-pedidos360}"

# --- 1. Compilar JAR ---
./gradlew bootJar --no-daemon
JAR="$(ls -t build/libs/rabbitmq-admin-*.jar | head -1)"
echo "JAR a desplegar: $JAR"

# --- 2. Subir JAR a la EC2 ---
scp -i "$KEY_PATH" "$JAR" "ec2-user@${ADMIN_IP}:${REMOTE_DIR}/"

# --- 3. Reconstruir imagen y relanzar con env vars de prod ---
ssh -i "$KEY_PATH" "ec2-user@${ADMIN_IP}" \
  "RABBITMQ_HOST='${RABBITMQ_HOST}' RABBITMQ_PORT='${RABBITMQ_PORT}' RABBITMQ_USER='${RABBITMQ_USER}' RABBITMQ_PASS='${RABBITMQ_PASS}' bash -s" <<'EOF'
set -e
cd /home/ec2-user
docker build -f Dockerfile -t rabbitmq-admin:latest .
docker rm -f rabbitmq-admin || true
docker run -d --name rabbitmq-admin --restart unless-stopped --network appnet \
  -p 8083:8083 \
  -e RABBITMQ_HOST="$RABBITMQ_HOST" \
  -e RABBITMQ_PORT="$RABBITMQ_PORT" \
  -e RABBITMQ_USER="$RABBITMQ_USER" \
  -e RABBITMQ_PASS="$RABBITMQ_PASS" \
  rabbitmq-admin:latest
echo "--- contenedores ---"
docker ps --format '{{.Names}} {{.Status}}'
echo "--- health (hasta 60s) ---"
for i in $(seq 1 12); do
  if curl -sf http://localhost:8083/actuator/health > /dev/null; then
    echo "rabbitmq-admin UP"
    exit 0
  fi
  sleep 5
done
echo "rabbitmq-admin NO respondio health a tiempo (revisar: docker logs rabbitmq-admin)"
exit 1
EOF
