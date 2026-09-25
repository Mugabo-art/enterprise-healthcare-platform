# --- Build stage ---
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npm run build

# --- Runtime stage: static files served by unprivileged nginx (non-root, listens on 8080) ---
FROM nginxinc/nginx-unprivileged:1.31-alpine
# Pull in Alpine security patches that landed after the base image was published (e.g. OpenSSL CVEs).
USER root
RUN apk upgrade --no-cache
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
# the unprivileged nginx user
USER 101
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s CMD wget -qO- http://127.0.0.1:8080/ >/dev/null || exit 1
