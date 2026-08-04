# ReconX — Team Avani, Vigna, Nandini, Shubhang, Gaurav — Retrospective

## What worked?
- Splitting ownership by layer early (backend/Kafka, frontend, DevOps/CI) meant everyone had a clear file to defend during the code walkthrough instead of a scramble on Day 10
- Running the smoke test (`scripts/smoke-test.sh`) after every infra change on Day 10 caught broken healthchecks before they reached CI, not during the demo
- Keeping the Liquibase changelog append-only from early on saved us from a schema-migration headache later in the week
- Reviewing PRs in pairs before merging into `develop` caught a few RBAC edge cases before they shipped

## What didn't?
- Underestimated how long JWT/RBAC wiring (Day 5) would take — ran well past the planned slot and pushed later tickets back
- Left the Grafana dashboard provisioning (Day 6/10) until close to the end, so we were tuning panels the same day we needed screenshots for the README and deck
- Didn't rehearse the live demo flow end-to-end until Day 10 itself, so the first run surfaced avoidable issues (Kafka not warm, wrong Swagger port in notes) that a rehearsal would have caught earlier
- A couple of feature branches sat unreviewed for longer than planned, so `develop` briefly diverged from what people expected

## What would you change?
- Lock the Postgres schema and Liquibase changelog structure by end of Day 1 and treat later changes as exceptions, not the default
- Start Docker/CI work (Day 10) a day earlier where possible instead of compressing containerization, quality gates, and demo prep into one day
- Do the first full demo rehearsal at least a day before the live slot, not the same afternoon
- Rotate who drives vs. who reviews more deliberately, so knowledge of each layer (Kafka, RBAC, React hooks) isn't concentrated in one person

## What surprised you?
- How much faster debugging got once Prometheus/Grafana were wired up — spotting a spike in the request-rate panel was faster than digging through logs
- How strict the JaCoCo 85% coverage gate was in practice — it caught a genuinely untested error-handling branch, not just a formality
- How much of the "hard part" of Kafka wasn't the producer/consumer code but getting the DLQ and listener config right
- How much smoother the live demo felt once we had a rehearsed fallback (screen recording) for every step, rather than hoping everything would just work

## Technical notes for the next cohort
- Double-check the Swagger UI port against what's actually in `application.yml`/`docker-compose.yml` before it goes in the README — small mismatches like this eat review time
- `docker compose up -d` on a cold cache takes noticeably longer than on a warm one — budget for that before a live demo, not during it
- Pre-stage every terminal command you'll run live (login, `psql`, curl) in your shell history beforehand — typos live on stage cost real minutes
- Take the Grafana screenshots (baseline / under load / recovery) on the same day you write the README section referencing them — stale screenshots are an easy, avoidable miss

## Team
- Lead:        Avani
- Backend:     Vigna (API/controllers), Nandini (Kafka/reconciliation)
- Frontend:    Shubhang
- DevOps/CI:   Gaurav

---
*Linked from the README's `## Team` section.*
