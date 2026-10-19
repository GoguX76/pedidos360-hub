#!/usr/bin/env bash
# Deploy del microservicio RABBITMQ-ADMIN.
#
# Se despliega en la MISMA EC2 que el broker (no hay instancia Admin propia):
#   ADMIN_IP = 3.233.100.195 (EC2 del cluster RabbitMQ).
#
# Uso (Git Bash en Windows, desde backend/rabbitmq-admin):
#   ./deploy-ec2.sh
#
# Que hace:
#   1. Compila el JAR localmente (./gradlew bootJar).
#   2. Sube el JAR a la EC2 del broker (el Dockerfile runtime-only ya vive en
#      /home/ec2-user/Dockerfile y NO se pisa).
#   3. Reconstruye la imagen y relanza el contenedor de entorno de produccion.
#
# RED: el admin se conecta al broker por la red Docker del compose
# (rabbitnet, declarada con ese nombre exacto en infra/rabbitmq/docker-compose.yml)
# y resuelve el nodo como 'rabbit1'. Si por algun motivo no se puede adjuntar a
# esa red, se puede sobreescribir RABBITMQ_HOST=172.31.47.36 (IP privada) y
# ADMIN_NETWORK=appnet.
#
# IMPORTANTE: sin SPRING_PROFILES_ACTIVE=prod + RABBITMQ_HOST/USER/PASS el
# contenedor arranca en perfil 'dev' con CORS de localhost. No omitir las -e.
set -euo pipefail

# --- Configuracion (sobrescribible por entorno) ---
# Ruta a la llave SSH (.pem). Obligatoria: exporta KEY_PATH antes de correr.
KEY_PATH="${KEY_PATH:?Exporta KEY_PATH con la ruta de tu llave .pem}"
ADMIN_IP="${ADMIN_IP:-3.233.100.195}"
REMOTE_DIR="/home/ec2-user"
SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"
RABBITMQ_HOST="${RABBITMQ_HOST:-rabbit1}"
RABBITMQ_PORT="${RABBITMQ_PORT:-5672}"
RABBITMQ_USER="${RABBITMQ_USER:-pedidos}"
RABBITMQ_PASS="${RABBITMQ_PASS:-pedidos360}"
ADMIN_NETWORK="${ADMIN_NETWORK:-rabbitnet}"

# --- 1. Compilar JAR ---
./gradlew bootJar --no-daemon
JAR="$(ls -t build/libs/rabbitmq-admin-*.jar | head -1)"
echo "JAR a desplegar: $JAR"

# --- 2. Subir JAR a la EC2 ---
scp -i "$KEY_PATH" "$JAR" "ec2-user@${ADMIN_IP}:${REMOTE_DIR}/"

# --- 3. Reconstruir imagen y relanzar con env vars de prod ---
ssh -i "$KEY_PATH" "ec2-user@${ADMIN_IP}" \
  "SPRING_PROFILES_ACTIVE='${SPRING_PROFILES_ACTIVE}' RABBITMQ_HOST='${RABBITMQ_HOST}' RABBITMQ_PORT='${RABBITMQ_PORT}' RABBITMQ_USER='${RABBITMQ_USER}' RABBITMQ_PASS='${RABBITMQ_PASS}' ADMIN_NETWORK='${ADMIN_NETWORK}' bash -s" <<'EOF'
set -e
cd /home/ec2-user
if ! docker network inspect "$ADMIN_NETWORK" > /dev/null 2>&1; then
  echo "ERROR: la red Docker '$ADMIN_NETWORK' no existe."
  echo "Levanta primero el cluster: cd /home/ec2-user/rabbitmq && docker compose up -d"
  exit 1
fi
docker build -f Dockerfile -t rabbitmq-admin:latest .
docker rm -f rabbitmq-admin || true
docker run -d --name rabbitmq-admin --restart unless-stopped --network "$ADMIN_NETWORK" \
  -p 8083:8083 \
  -e SPRING_PROFILES_ACTIVE="$SPRING_PROFILES_ACTIVE" \
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
