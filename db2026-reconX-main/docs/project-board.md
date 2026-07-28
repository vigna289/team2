# Project Board Setup — Day 1 (TICKET-ADV016)

> Reference doc for the Jira / GitHub Projects board. The board itself lives
> in the external tool, not in git — this file is committed so the structure
> is reviewable and trainers/teammates can see what was set up without
> opening the tool.

## Epics

```
Epic: RECONX-E1 — Day 1: Architecture & Setup
  Cards: TICKET-ADV001 .. TICKET-ADV004

Epic: RECONX-E2 — Day 1: Schema & Analytics
  Cards: TICKET-ADV006 .. TICKET-ADV011

Epic: RECONX-E3 — Day 1: Liquibase & Tooling
  Cards: TICKET-ADV012 .. TICKET-ADV017
```

## Columns

```
Backlog → To Do → In Progress → In Review (PR open) → Done
```

## Per-card fields

- **Exercise ID** — exact match to the `TICKET-ADVxxx` tag used in codebase TODO blocks (e.g. `TICKET-ADV007`)
- **Estimate** — story points: 1, 2, 3, 5, 8
- **Owner** — assignee
- **Linked PR** — link to the PR that closes the card
- **Acceptance criteria** — copied verbatim from this guide's "Done when" block for that ticket

## GitHub Projects field schema

If using GitHub Projects, paste this into the project's custom fields settings:

```yaml
fields:
  - name: Exercise ID
    type: text
  - name: Estimate
    type: single_select
    options: [1, 2, 3, 5, 8]
  - name: Owner
    type: assignee
  - name: Linked PR
    type: text
  - name: Status
    type: single_select
    options: [Backlog, To Do, In Progress, In Review, Done]
```

## Setup checklist

- [ ] Board created (Jira or GitHub Projects)
- [ ] 3 epics created (Architecture & Setup / Schema & Analytics / Liquibase & Tooling)
- [ ] Columns configured: Backlog → To Do → In Progress → In Review → Done
- [ ] 17 cards created — one per TICKET-ADV001 through TICKET-ADV017
- [ ] Each card tagged with Exercise ID, Estimate, Owner, Acceptance criteria
- [ ] At least one card moved end-to-end through every column
