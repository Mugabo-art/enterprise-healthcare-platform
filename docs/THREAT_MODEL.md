# Threat Model (STRIDE)
## Enterprise Healthcare Platform

STRIDE categorizes threats by: **S**poofing, **T**ampering, **R**epudiation, **I**nformation disclosure, **D**enial of service, **E**levation of privilege.

## 1. Assets
- Patient PHI (demographics, medical history, lab results)
- Credentials & session tokens
- Prescription and pharmacy inventory data (diversion risk)
- Billing/payment records

## 2. Trust Boundaries
1. Browser ↔ API (public internet or hospital LAN)
2. API ↔ Database
3. API ↔ Kafka ↔ downstream consumers
4. API ↔ External AI provider (for the AI module)

## 3. Threats & Mitigations

| # | Category | Threat | Mitigation |
|---|---|---|---|
| 1 | Spoofing | Attacker impersonates a clinician using stolen credentials | Bcrypt-hashed passwords, optional TOTP MFA, account lockout after repeated failures, short-lived JWTs |
| 2 | Spoofing | Refresh token theft used to mint new access tokens indefinitely | Refresh tokens stored hashed, rotated on each use, revocable server-side, bound to a device fingerprint (future work) |
| 3 | Tampering | Client tampers with JWT claims (e.g. changes role) | JWTs are signed (HMAC/RSA); signature verified server-side on every request; role is never trusted from the client alone |
| 4 | Tampering | SQL injection via crafted input | All queries via JPA/parameterized queries, no string-concatenated SQL |
| 5 | Repudiation | A clinician denies having accessed/modified a patient record | Audit log table capturing user_id, action, resource, timestamp for every PHI read/write |
| 6 | Information Disclosure | PHI exposed via verbose error messages or stack traces | Global exception handler returns generic error bodies in production; stack traces only in dev profile |
| 7 | Information Disclosure | PHI intercepted in transit | TLS enforced everywhere (ingress terminates TLS; internal traffic within the cluster mesh is also encrypted in the target deployment) |
| 8 | Information Disclosure | Over-broad role permissions let a nurse read billing data | RBAC enforced per-endpoint via Spring Security method security, reviewed against SRS role matrix |
| 9 | Denial of Service | Login endpoint brute-forced or flooded | Rate limiting per account/IP on `/api/auth/*`; Redis-backed counters |
| 10 | Denial of Service | Kafka consumer lag/outage blocks notifications | Notification module treated as best-effort/async — failures here never block the primary clinical transaction |
| 11 | Elevation of Privilege | A lab technician calls a billing endpoint directly | Every endpoint has an explicit `@PreAuthorize` role check; default-deny, not default-allow |
| 12 | Elevation of Privilege | Compromised AI-module API key used to exfiltrate data via prompt injection in patient notes | AI module receives only the minimum fields needed for summarization, never raw DB credentials or unrelated patients' data; outputs are read-only/advisory (see SRS FR-9.3) |

## 4. Data Classification & Handling
- PHI fields are never logged (structured logging redacts known PHI field names).
- Backups encrypted at rest; backup access requires admin role + audit entry.
- Least-privilege DB roles: the application's DB user cannot `DROP TABLE` or alter schema — migrations run under a separate elevated role.

## 5. Residual Risk / Future Work
- No hardware security key (WebAuthn) support yet — TOTP MFA only.
- No field-level encryption for PHI columns yet (relies on at-rest disk encryption + TLS in transit); noted as a hardening item before any real deployment.
- Multi-tenancy (if added later) would need per-tenant row-level security review.
