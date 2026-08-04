# ReconX — Architecture (C4 Model)

Deliverable for **TICKET-ADV160**. Consolidates the Context, Container, and
Component diagrams from ADV002–ADV004 into a single reference doc for the
README (ADV161) and demo deck (ADV162). Render any block at
https://mermaid.live to view or export as PNG/SVG.

---

## Level 1 — System Context

Who uses ReconX and what external systems it talks to.

```mermaid
C4Context
    title C4 Context — ReconX Enterprise Trade Reconciliation Platform

    Person(traderUser, "Trader", "Books and amends trades; investigates breaks.")
    Person(reconAnalyst, "Recon Analyst", "Resolves daily reconciliation breaks.")
    Person(opsAdmin, "Ops Admin", "Manages users, audits activity.")
    Person(complianceUser, "Compliance Officer", "Reads audit log + reports only.")

    System(reconx, "ReconX", "Internal trade reconciliation platform. Auto-matches internal vs external trade records, surfaces breaks, tracks resolution SLAs.")

    System_Ext(internalOMS, "Internal OMS", "Source of internal trade records (intra-day Kafka feed).")
    System_Ext(counterpartySFTP, "Counterparty Trade Files", "EOD CSV feeds from custodian/counterparties via SFTP.")
    System_Ext(bloombergPricing, "Bloomberg Pricing", "Reference market data for break investigation.")
    System_Ext(emailGateway, "Corporate Email Gateway", "Sends break-resolution notifications to Ops.")
    System_Ext(ssoIdP, "Corporate SSO (Entra ID)", "Issues JWT after OIDC login.")
    System_Ext(grafana, "Grafana / Prometheus", "Scrapes metrics for SRE dashboards and alerts.")

    Rel(traderUser, reconx, "Books trades, views breaks", "HTTPS")
    Rel(reconAnalyst, reconx, "Resolves breaks", "HTTPS")
    Rel(opsAdmin, reconx, "User admin, audit", "HTTPS")
    Rel(complianceUser, reconx, "Reads audit log + reports", "HTTPS, read-only")

    Rel(internalOMS, reconx, "Streams trade events", "Kafka topic: trade-events")
    Rel(counterpartySFTP, reconx, "Drops EOD trade CSVs", "SFTP poll, 5-min interval")
    Rel(reconx, bloombergPricing, "Fetches reference prices", "HTTPS, REST")
    Rel(reconx, emailGateway, "Sends break notifications", "SMTP")
    Rel(reconx, ssoIdP, "Validates user", "OIDC, HTTPS")
    Rel(grafana, reconx, "Scrapes /actuator/prometheus", "HTTPS")
```

**Verify:** exactly one `System(reconx, ...)` box; four `Person(...)` nodes; ~six `System_Ext(...)` nodes; every `Rel(...)` carries an intent + protocol label.

---

## Level 2 — Containers

Zooming into ReconX itself: the deployable pieces and how they talk.

```mermaid
C4Container
    title C4 Container — ReconX

    Person(user, "User", "Trader / Analyst / Admin")
    System_Ext(omsKafka, "Internal OMS", "Upstream trade source")
    System_Ext(sso, "Corporate SSO", "OIDC IdP")

    System_Boundary(reconxBoundary, "ReconX") {
        Container(reactSpa, "Recon UI", "React 19 + Vite", "Live trade feed via SSE; trades + breaks tables; admin views.")
        Container(api, "recon-service API", "Java 25 + Spring Boot 3", "REST API. JWT auth, RBAC, validation, exposes /actuator/prometheus.")
        Container(reconEngine, "Reconciliation Engine", "Spring + CompletableFuture", "Async batch + streaming match logic. Writes recon_breaks.")
        ContainerDb(postgres, "PostgreSQL 16", "Liquibase-managed", "Partitioned trades, recon_breaks, audit_log, mat. views.")
        ContainerQueue(kafka, "Apache Kafka", "3 topics + DLQs", "trade-events, recon-results, system-alerts. DLQ per topic.")
        Container(prom, "Prometheus", "TSDB", "Scrapes the API every 15s.")
        Container(graf, "Grafana", "Dashboard", "Pre-provisioned dashboards.")
    }

    Rel(user, reactSpa, "Uses", "HTTPS")
    Rel(reactSpa, api, "REST + SSE", "HTTPS / JSON")
    Rel(reactSpa, sso, "Login (OIDC)", "HTTPS")
    Rel(api, postgres, "Reads + writes", "JDBC")
    Rel(api, kafka, "Publishes trade-events", "Kafka protocol")
    Rel(reconEngine, kafka, "Consumes trade-events", "Kafka protocol")
    Rel(reconEngine, postgres, "Writes recon_breaks", "JDBC")
    Rel(omsKafka, kafka, "Streams trades", "Kafka MirrorMaker")
    Rel(prom, api, "Scrapes /actuator/prometheus", "HTTPS")
    Rel(graf, prom, "Queries", "HTTPS / PromQL")
```

**Verify:** exactly 7 boxes inside the boundary (React SPA, API, recon engine, Postgres, Kafka, Prometheus, Grafana); Postgres uses `ContainerDb`, Kafka uses `ContainerQueue`; User/OMS/SSO sit outside the boundary.

---

## Level 3 — Components (recon-service API)

Zooming into the API container itself.

```mermaid
C4Component
    title C4 Component — recon-service API

    Container_Ext(reactSpa, "Recon UI", "React")
    ContainerDb_Ext(postgres, "PostgreSQL")
    ContainerQueue_Ext(kafka, "Kafka")

    Container_Boundary(api, "recon-service API") {
        Component(authCtl, "AuthController", "Spring REST", "/api/auth/login, /refresh")
        Component(tradeCtl, "TradeController", "Spring REST", "/api/v1/trades CRUD")
        Component(reconCtl, "ReconController", "Spring REST", "/api/v1/recon/breaks")
        Component(auditCtl, "AuditController", "Spring REST", "/api/v1/audit (read-only)")

        Component(jwtFilter, "JwtAuthFilter", "OncePerRequestFilter", "Parses + validates JWT, sets SecurityContext")
        Component(rbac, "MethodSecurity", "@PreAuthorize", "Role gate per endpoint")

        Component(tradeSvc, "TradeService", "@Service", "Trade lifecycle business rules")
        Component(reconSvc, "ReconciliationService", "@Service", "Match + break detection")
        Component(auditSvc, "AuditService", "@Service", "Writes audit_log via trigger or app-layer hook")

        Component(tradeRepo, "TradeRepository", "JpaRepository + Specs", "Paged + filtered queries")
        Component(reconRepo, "ReconBreakRepository", "JpaRepository", "Break queries")
        Component(auditRepo, "AuditRepository", "JpaRepository", "Read-only audit queries")

        Component(producer, "TradeEventProducer", "KafkaTemplate", "Publishes trade-events on commit")
        Component(consumer, "ReconResultConsumer", "@KafkaListener", "Consumes recon-results from engine")
    }

    Rel(reactSpa, authCtl, "POST /login", "HTTPS")
    Rel(reactSpa, tradeCtl, "REST", "HTTPS + JWT")
    Rel(reactSpa, reconCtl, "REST", "HTTPS + JWT")
    Rel(reactSpa, auditCtl, "REST", "HTTPS + JWT")

    Rel(jwtFilter, rbac, "Sets SecurityContext")
    Rel(tradeCtl, tradeSvc, "calls")
    Rel(reconCtl, reconSvc, "calls")
    Rel(auditCtl, auditSvc, "calls")

    Rel(tradeSvc, tradeRepo, "uses")
    Rel(reconSvc, reconRepo, "uses")
    Rel(auditSvc, auditRepo, "uses")

    Rel(tradeRepo, postgres, "JDBC")
    Rel(reconRepo, postgres, "JDBC")
    Rel(auditRepo, postgres, "JDBC")

    Rel(tradeSvc, producer, "emits event")
    Rel(producer, kafka, "publish trade-events")
    Rel(consumer, kafka, "subscribe recon-results")
    Rel(consumer, reconSvc, "callback")
```

**Verify:** title names the container being zoomed into; ~10–15 component boxes (4 controllers, `JwtAuthFilter`, `MethodSecurity`, 3 services, 3 repositories, 1 producer, 1 consumer); UI/Postgres/Kafka appear only as `*_Ext` at the edge; arrow flow reads UI → controllers → services → repositories/producer → DB/Kafka.

---

## CI/CD Pipeline

```mermaid
graph LR
    A["git push<br/>feature/* -> develop"] --> B["Lint"]
    B --> C["Unit + integration tests<br/>(Testcontainers)"]
    C --> D{"JaCoCo gate<br/>>= 85% line coverage"}
    D -- fail --> D1["Build fails<br/>PR blocked"]
    D -- pass --> E["Liquibase changelog<br/>validate"]
    E --> F["Docker build<br/>backend + frontend"]
    F --> G["Push images to GHCR<br/>tags: sha, latest"]

    H["git tag -a v1.0.0<br/>git push origin v1.0.0"] --> I["Tag-triggered workflow<br/>on.push.tags: v*"]
    I --> J["Re-run build + tests"]
    J --> K["Push images to GHCR<br/>tag: v1.0.0"]

    G -.merge to main.-> H

    classDef gate fill:#FFF6E5,stroke:#C77700,stroke-width:1px;
    classDef fail fill:#FBE4E4,stroke:#C0392B,stroke-width:1px;
    classDef stage fill:#EAF1FE,stroke:#1668E3,stroke-width:1px;
    classDef release fill:#E9F7EF,stroke:#1E8449,stroke-width:1px;

    class D gate;
    class D1 fail;
    class A,B,C,E,F,G stage;
    class H,I,J,K release;
```

**Verify:** the develop-branch path (lint → test → coverage gate →
Liquibase validate → Docker build → GHCR push) is separate from the
tag-triggered release path (tag push → re-build → GHCR push tagged
`v1.0.0`); the coverage gate has an explicit fail branch to a blocked PR.

---

## Source tickets

| Level | Ticket | Original file |
|-------|--------|----------------|
| Context | ADV002 | `db/diagrams/c4-context.md` |
| Container | ADV003 | `db/diagrams/c4-container.md` |
| Component | ADV004 | `db/diagrams/c4-component.md` |
| CI/CD | ADV160 | (this file, generated) |

This file (`db/diagrams/architecture.md`) is the ADV160 consolidation point —
the README (ADV161, Architecture + CI/CD sections) and demo deck (ADV162,
slides 3 and 7) embed rendered PNGs sourced from here
(`docs/images/c4-container.png`, `docs/images/cicd-pipeline.png`, etc.).
