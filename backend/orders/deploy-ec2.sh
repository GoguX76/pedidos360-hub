#!/usr/bin/env bash
# Deploy del microservicio ORDERS a su EC2 (50.19.219.43).
#
# Uso (Git Bash en Windows, desde backend/orders):
#   ./deploy-ec2.sh
#
# Que hace:
#   1. Compila el JAR localmente (./gradlew bootJar).
#   2. Sube el JAR a la EC2 (el Dockerfile runtime-only ya vive en
#      /home/ec2-user/Dockerfile y NO se pisa).
#   3. Reconstruye la imagen y relanza el contenedor con las variables
#      de entorno de produccion.
#
# IMPORTANTE: sin SPRING_PROFILES_ACTIVE=prod + DB_URL/DB_USER/DB_PASSWORD
# el contenedor arranca con el perfil 'dev' (H2 en memoria): los pedidos
# se pierden en cada restart y no usa PostgreSQL. No omitir las -e.
set -euo pipefail

# --- Configuracion (sobrescribible por entorno) ---
KEY_PATH="${KEY_PATH:-C:/Users/Scor/Downloads/pedidos360.pem}"
ORDERS_IP="${ORDERS_IP:-50.19.219.43}"
REMOTE_DIR="/home/ec2-user"
SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"
DB_URL="${DB_URL:-jdbc:postgresql://pg-orders:5432/pedidos360}"
DB_USER="${DB_USER:-pedidos}"
DB_PASSWORD="${DB_PASSWORD:-pedidos360}"

# --- 1. Compilar JAR ---
./gradlew bootJar --no-daemon
JAR="$(ls -t build/libs/orders-*.jar | head -1)"
echo "JAR a desplegar: $JAR"

# --- 2. Subir JAR a la EC2 ---
scp -i "$KEY_PATH" "$JAR" "ec2-user@${ORDERS_IP}:${REMOTE_DIR}/"

# --- 3. Reconstruir imagen y relanzar con env vars de prod ---
# (las VAR='...' van como prefijo del comando remoto: bash -s las recibe
# como variables de entorno en la EC2; el heredoc queda quoted para que
# nada se expanda en la maquina local)
ssh -i "$KEY_PATH" "ec2-user@${ORDERS_IP}" \
  "SPRING_PROFILES_ACTIVE='${SPRING_PROFILES_ACTIVE}' DB_URL='${DB_URL}' DB_USER='${DB_USER}' DB_PASSWORD='${DB_PASSWORD}' bash -s" <<'EOF'
set -e
cd /home/ec2-user
docker build -f Dockerfile -t orders:latest .
docker rm -f orders || true
docker run -d --name orders --restart unless-stopped --network appnet \
  -p 8081:8081 \
  -e SPRING_PROFILES_ACTIVE="$SPRING_PROFILES_ACTIVE" \
  -e DB_URL="$DB_URL" \
  -e DB_USER="$DB_USER" \
  -e DB_PASSWORD="$DB_PASSWORD" \
  orders:latest
echo "--- contenedores ---"
docker ps --format '{{.Names}} {{.Status}}'
echo "--- health (hasta 60s) ---"
for i in $(seq 1 12); do
  if curl -sf http://localhost:8081/actuator/health > /dev/null; then
    echo "orders UP"
    exit 0
  fi
  sleep 5
done
echo "orders NO respondio health a tiempo (revisar: docker logs orders)"
exit 1
EOF
