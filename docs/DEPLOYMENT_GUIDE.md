# Deployment Guide
## Enterprise Healthcare Platform

Three stages, in increasing order of production-readiness.

## Stage 1 — Local (Docker Compose)

```bash
cp .env.example .env      # fill in secrets
docker compose up --build
```
Brings up: `frontend`, `backend`, `postgres`, `redis`, `kafka` (+ zookeeper), `prometheus`, `grafana`. See [`docker-compose.yml`](../docker-compose.yml).

To reset the database: `docker compose down -v` (drops volumes).

## Stage 2 — Single Cloud VM (smoke-test staging)

1. Provision a VM (e.g. a small EC2/DigitalOcean instance) with Docker + Docker Compose installed.
2. `git clone` the repo, set `.env` with production-like secrets (real DB password, JWT signing key, SMTP credentials).
3. `docker compose -f docker-compose.yml up -d`.
4. Put the VM behind a reverse proxy (Caddy/Nginx) for TLS termination via Let's Encrypt.

This stage is for demoing the whole stack cheaply — not for real patient data.

## Stage 3 — Kubernetes (target production topology)

Manifests live in [`/k8s`](../k8s):

```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/postgres.yaml
kubectl apply -f k8s/redis.yaml
kubectl apply -f k8s/backend-deployment.yaml
kubectl apply -f k8s/frontend-deployment.yaml
kubectl apply -f k8s/ingress.yaml
```

Recommended: manage `k8s/` manifests via **ArgoCD** instead of manual `kubectl apply`:

1. Install ArgoCD in the cluster (`kubectl create namespace argocd && kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml`).
2. Create an ArgoCD `Application` pointing at this repo's `k8s/` directory.
3. ArgoCD continuously reconciles cluster state to match what's committed — a `git push` to `main` (after CI passes) is the only deploy action needed; no manual `kubectl apply` in steady state.

### Secrets
Never commit real secrets. In Kubernetes, use `Secret` objects (or an external secrets manager like Vault/Sealed Secrets) — `k8s/*.yaml` reference secret names, not values, via `envFrom`/`secretKeyRef`.

### Observability
Prometheus scrapes `/actuator/prometheus` on the backend (see `monitoring/prometheus.yml`). Import the sample Grafana dashboard JSON from `monitoring/grafana-dashboards/` for API latency, error rate, and JVM metrics.

### Zero-downtime deploys
The backend Deployment uses a `RollingUpdate` strategy with a `readinessProbe` on `/actuator/health/readiness`, so new pods only receive traffic once ready.

## CI/CD Pipeline

`.github/workflows/ci-cd.yml` runs on every push/PR to `main`:
1. Backend: `mvn verify` (compiles, runs unit + integration tests)
2. Frontend: `npm ci && npm run build && npm test`
3. (On `main` only) Builds and pushes Docker images tagged with the commit SHA to a container registry
4. Deploy is intentionally left to ArgoCD (GitOps) rather than the CI pipeline pushing to the cluster directly — keeps deploy credentials out of CI entirely.
