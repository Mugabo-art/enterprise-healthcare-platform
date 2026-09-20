# Grafana dashboards

Drop exported dashboard JSON here (Grafana → Dashboard → Share → Export).
Suggested starter panels once the backend has real traffic:
- API p50/p95/p99 latency (from `http_server_requests_seconds` via Micrometer)
- Error rate by endpoint (5xx / total)
- JVM heap usage and GC pause time
- Login failure rate (proxy for brute-force attempts, ties to docs/THREAT_MODEL.md #9)
