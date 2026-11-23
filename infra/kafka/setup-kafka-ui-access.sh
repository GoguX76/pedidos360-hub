#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Uso: $0 <IPv4-publica-de-la-EC2>" >&2
  exit 2
fi

PUBLIC_IP="$(python3 -c 'import ipaddress,sys; print(ipaddress.IPv4Address(sys.argv[1]))' "$1")"
SECRETS_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)/secrets/kafka-ui"

read -r -p "Usuario para Kafka UI: " AUTH_USER
if [[ ! "$AUTH_USER" =~ ^[A-Za-z0-9._-]{3,64}$ ]]; then
  echo "El usuario debe tener 3-64 caracteres: letras, numeros, punto, guion o guion bajo." >&2
  exit 1
fi

read -r -s -p "Contrasena (minimo 16 caracteres): " AUTH_PASSWORD
printf '\n'
if [[ ${#AUTH_PASSWORD} -lt 16 ]]; then
  echo "La contrasena debe tener al menos 16 caracteres." >&2
  exit 1
fi

umask 077
mkdir -p "$SECRETS_DIR"
openssl req -x509 -nodes -days 365 -newkey rsa:3072 \
  -keyout "$SECRETS_DIR/tls.key" \
  -out "$SECRETS_DIR/tls.crt" \
  -subj "/CN=${PUBLIC_IP}" \
  -addext "subjectAltName=IP:${PUBLIC_IP}" \
  -addext "extendedKeyUsage=serverAuth"

PASSWORD_HASH="$(printf '%s' "$AUTH_PASSWORD" | openssl passwd -apr1 -stdin)"
printf '%s:%s\n' "$AUTH_USER" "$PASSWORD_HASH" > "$SECRETS_DIR/htpasswd"
unset AUTH_PASSWORD PASSWORD_HASH
chmod 755 "$SECRETS_DIR"
chmod 600 "$SECRETS_DIR/tls.key"
chmod 644 "$SECRETS_DIR/tls.crt"
chmod 644 "$SECRETS_DIR/htpasswd"

echo "Certificado y autenticacion generados en $SECRETS_DIR"
echo "El certificado es autofirmado; el navegador mostrara una advertencia."
