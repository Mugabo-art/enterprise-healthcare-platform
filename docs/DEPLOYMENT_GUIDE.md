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
# Create the real secrets first (see "Production configuration" below), then:
kubectl apply -k k8s/     # kustomize: all manifests, images pinned by CI to the built commit SHA
```

Recommended: manage `k8s/` manifests via **ArgoCD** instead of manual `kubectl apply`:

1. Install ArgoCD in the cluster (`kubectl create namespace argocd && kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml`).
2. Create an ArgoCD `Application` pointing at this repo's `k8s/` directory.
3. ArgoCD continuously reconciles cluster state to match what's committed — a `git push` to `main` (after CI passes) is the only deploy action needed; no manual `kubectl apply` in steady state.

### Secrets
Never commit real secrets. In Kubernetes, use `Secret` objects (or an external secrets manager like Vault/Sealed Secrets) — `k8s/*.yaml` reference secret names, not values, via `envFrom`/`secretKeyRef`.

### Observability
In the `prod` profile actuator moves to the cluster-internal management port **8081** (never routed by the ingress); pods carry `prometheus.io/*` annotations and the `allow-prometheus-to-backend-management` NetworkPolicy admits the `monitoring` namespace. Locally (docker-compose) Prometheus scrapes `backend:8080` (see `monitoring/prometheus.yml`). Logs are one JSON object per line in `prod` and every line carries a `requestId` (also returned as the `X-Request-Id` response header and stored on audit rows) for end-to-end tracing. Import the sample Grafana dashboard JSON from `monitoring/grafana-dashboards/` for API latency, error rate, and JVM metrics.

### Zero-downtime deploys
The backend Deployment uses a `RollingUpdate` strategy with a `readinessProbe` on `/actuator/health/readiness`, so new pods only receive traffic once ready.

## CI/CD Pipeline

`.github/workflows/ci-cd.yml` runs on every push/PR to `main`:
1. Backend: `mvn verify` (unit tests, full-stack security tests on H2, and Testcontainers migration tests on real PostgreSQL 16)
2. Frontend: lint, tests, build, `npm audit` on runtime dependencies
3. Security scan: Trivy over dependencies, committed secrets and Dockerfile/Kubernetes misconfiguration; CodeQL (Java + JS) in its own workflow; Dependabot PRs weekly
4. (On `main` only) Builds both images, **scans them with Trivy before pushing**, then pushes them tagged with the commit SHA only (no `:latest`)
5. (On `main` only) Commits the new SHA into `k8s/kustomization.yaml`. Deploy is intentionally left to ArgoCD (GitOps) rather than the CI pipeline pushing to the cluster directly — keeps deploy credentials out of CI entirely. If `main` is branch-protected, allow `github-actions[bot]` to push or convert this step into an auto-merged PR.

## Production configuration

Run with `SPRING_PROFILES_ACTIVE=prod` (the Kubernetes manifests already do). The prod profile has **no fallback defaults for secrets** and the backend refuses to start (`ProductionConfigValidator`) if any of these is missing or weak:

| Variable | Requirement |
|---|---|
| `JWT_SECRET` | ≥ 32 random characters, not a placeholder (`openssl rand -base64 48`) |
| `DB_PASSWORD` | ≥ 12 characters, not `changeme` |
| `CORS_ALLOWED_ORIGINS` | the real https origin(s), comma-separated; no `*`, no `localhost` |
| `BOOTSTRAP_ADMIN_EMAIL` / `BOOTSTRAP_ADMIN_PASSWORD` | optional; creates the first ADMIN when the users table is empty (password ≥ 12 chars). Remove after first start and rotate |

- **No seeded accounts.** The demo admin (`admin@hospital.test`) lives in `db/dev` and is only loaded by the `dev` profile (docker-compose sets it). Production Flyway only reads `db/migration`.
- **Account creation.** `POST /api/auth/register` is public for PATIENT accounts only; every other role requires an authenticated ADMIN.
- **Swagger / API docs are disabled** in prod; actuator is off the public port.
- **Audit trail.** Every authenticated `/api` call and every login/logout/registration is written to the append-only `audit_log` table (a database trigger rejects UPDATE/DELETE). Admins query it via `GET /api/audit?actorId=…` or `?resourceType=patients&resourceId=…`. Define a retention/archival policy that meets your regulator's requirements.
- **Rate limiting.** Per-client-IP limit on `/api/auth/*` (`AUTH_RATE_LIMIT_PER_MINUTE`, default 30, Redis-backed) plus per-account lockout after 5 failed logins/MFA attempts, plus an ingress-level limit.
- **Sessions.** Access tokens live 15 min; refresh tokens rotate on every use, and presenting an already-used refresh token revokes every session for that user (theft detection). Expired refresh tokens are purged nightly.

### Backup & restore
`k8s/postgres-backup.yaml` takes a nightly `pg_dump -Fc` onto a dedicated volume (7 days kept). That protects against mistakes, **not** against losing the cluster — copy the dumps off-cluster and rehearse a restore:

```bash
kubectl -n healthcare-platform exec -it <postgres-pod> -- createdb -U healthplatform restore_test
kubectl -n healthcare-platform cp <backup-pod>:/backups/<file>.dump /tmp/restore.dump
pg_restore -h <host> -U healthplatform -d restore_test --no-owner /tmp/restore.dump
```

### Known limitations
- The frontend keeps tokens in `localStorage` (XSS-exposed by design); a strict CSP is set, but moving the refresh token to an `HttpOnly` cookie is the next hardening step.
- Redis has no password (isolated by NetworkPolicy); Kafka is referenced in config but not deployed by these manifests.
- Database high availability (replication/failover) is out of scope here — use a managed PostgreSQL for a real production workload.
