#!/bin/bash
set -e

domains=(api.yourdomain.com admin.yourdomain.com)
email="your-email@example.com"
staging=0 # Set to 1 for testing

echo "=== Inicializando certificados SSL Let's Encrypt ==="

# Create dummy certificates
for domain in "${domains[@]}"; do
  path="/etc/letsencrypt/live/$domain"
  mkdir -p "./certbot/conf/live/$domain"
  
  if [ ! -f "./certbot/conf/live/$domain/fullchain.pem" ]; then
    echo "### Creating dummy certificate for $domain ..."
    openssl req -x509 -nodes -newkey rsa:2048 -days 1 \
      -keyout "./certbot/conf/live/$domain/privkey.pem" \
      -out "./certbot/conf/live/$domain/fullchain.pem" \
      -subj "/CN=localhost"
  fi
done

# Start nginx
echo "### Starting nginx ..."
docker compose up -d nginx

# Delete dummy certificates
for domain in "${domains[@]}"; do
  echo "### Removing dummy certificate for $domain ..."
  docker compose run --rm --entrypoint "\
    rm -rf /etc/letsencrypt/live/$domain && \
    rm -rf /etc/letsencrypt/archive/$domain && \
    rm -rf /etc/letsencrypt/renewal/$domain.conf" certbot
done

# Request certificates
for domain in "${domains[@]}"; do
  echo "### Requesting Let's Encrypt certificate for $domain ..."
  
  if [ $staging != "0" ]; then staging_arg="--staging"; fi
  
  docker compose run --rm --entrypoint "\
    certbot certonly --webroot -w /var/www/certbot \
      $staging_arg \
      --email $email \
      --agree-tos \
      --no-eff-email \
      -d $domain" certbot
done

# Reload nginx
echo "### Reloading nginx ..."
docker compose exec nginx nginx -s reload

echo "=== SSL certificates configured successfully ==="

