# ReconX — Enterprise Trade Reconciliation Platform

Near-production-grade trade reconciliation platform built for the Deutsche
Bank TDI 2026 Advanced Track. Ops teams use it (in concept) to detect and
resolve mismatches between internal trade records and external
counterparty/custodian feeds in near-real-time — Java 25 / Spring Boot 3
backend, React 19 frontend, Kafka event streaming, JWT-backed RBAC, and a
CI/CD pipeline that ships Docker images to GHCR.

## Quick start

```bash
echo $GHCR_PAT | docker login ghcr.io -u <gh-username> --password-stdin
docker compose pull
docker compose up -d
```

Open http://localhost:5173 and log in as `trader@db.com / trader123`.

## Table of contents
- [Architecture](#architecture)
- [Tech stack](#tech-stack)
- [API documentation](#api-documentation)
- [Monitoring](#monitoring)
- [Kafka topics](#kafka-topics)
- [Load test results](#load-test-results)
- [CI/CD pipeline](#cicd-pipeline)
- [Deploy runbook](#deploy-runbook)
- [Default credentials](#default-credentials)
- [Troubleshooting](#troubleshooting)
- [Team](#team)

## Architecture

Three C4 levels — Context, Container, Component. Full source lives in
[`db/diagrams/architecture.md`](db/diagrams/architecture.md) (ADV160).

### System Context

![C4 Context diagram — ReconX](docs/images/c4-context.png)

### Containers

![C4 Container diagram — ReconX](docs/images/c4-container.png)

### Components (recon-service API)

![C4 Component diagram — recon-service API](docs/images/c4-component.png)

> Mermaid source for all three diagrams lives in
> [`db/diagrams/architecture.md`](db/diagrams/architecture.md) — regenerate
> these PNGs from there (mermaid.live export) if the architecture changes.

## Tech stack

- **Data:** PostgreSQL 16, Liquibase-managed migrations
- **Application:** Java 25, Spring Boot 3, Spring Security (JWT + RBAC)
- **Messaging:** Apache Kafka (3 topics + DLQ per topic)
- **UI:** React 19, Vite, SSE live feed
- **Observability:** Prometheus, Grafana

## API documentation

Swagger UI: `<backend-url>/swagger-ui.html`
> ⚠️ Placeholder — swap in the live URL once the backend is deployed for
> the demo (check the actual port in `application.yml` / `docker-compose.yml`
> before publishing this link).

## Monitoring

Prometheus scrapes `/actuator/prometheus` every 15s; Grafana dashboards are
pre-provisioned (request rate, latency, break count). Dashboard screenshots
were explicitly marked skip/optional by the instructor for this cohort, so
none are included here — this is a deliberate scope decision, not a missing
deliverable.

## Kafka topics

| Topic | Purpose |
|-------|---------|
| `trade-events` | Trades published by the API on commit |
| `recon-results` | Match/break results from the reconciliation engine |
| `system-alerts` | Operational alerts |
| DLQ (per topic) | Dead-letter queue for failed consumption |

## Load test results

_Pending ADV158 — k6 summary (200 VUs, p95 latency, throughput, error rate)
goes here._

## CI/CD pipeline

![CI/CD pipeline — ReconX](docs/images/cicd-pipeline.png)

On every push to `develop`: lint → unit/integration tests (Testcontainers)
→ JaCoCo gate (≥85% line coverage) → Liquibase changelog validate → Docker
build (backend + frontend) → push to GHCR tagged `sha` and `latest`. A
failed coverage gate blocks the PR. Tagging `v1.0.0` on `main` triggers a
separate re-build that pushes the same images tagged `v1.0.0`.

## Deploy runbook

```bash
echo $GHCR_PAT | docker login ghcr.io -u <gh-username> --password-stdin
docker compose pull
docker compose up -d
```

## Default credentials

> Dev profile only — never use these outside local/demo environments.

| Role | Username | Password |
|------|----------|----------|
| ADMIN | admin@db.com | admin123 |
| TRADER | trader@db.com | trader123 |
| VIEWER | viewer@db.com | viewer123 |
| RECON_ANALYST | recon@db.com | recon123 |

## Troubleshooting

- **Port conflicts:** confirm nothing else is bound to 5173 (frontend), the
  backend port, 9090 (Prometheus), 3000 (Grafana), or 9000 (Kafdrop).
- **GHCR auth failures:** re-run `docker login ghcr.io` with a PAT that has
  `read:packages` scope.
- **Kafka listener issues:** check the advertised listener config matches
  how you're connecting (host vs. container network).

## Team

See [`docs/retrospective.md`](docs/retrospective.md) for the full team
retrospective.

- Lead: Avani
- Backend: Vigna (API/controllers), Nandini (Kafka/reconciliation)
- Frontend: Shubhang
- DevOps/CI: Gaurav
