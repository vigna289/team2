# ReconX — Demo Runsheet (20 minutes, +5 min cushion)

Structure: 3 min context · 8 min live demo · 5 min code walkthrough · 4 min Q&A.
Every screen switch is timestamped so nobody switches mid-sentence.

| Time | Segment | Owner | Script / action |
|------|---------|-------|------------------|
| 0:00–0:30 | Title + intros | Lead | Say names and roles, nothing else |
| 0:30–3:00 | Problem + Architecture (slides 2–3) | Lead | Walk the mermaid diagram box by box |
| 3:00–4:00 | Tech stack (slide 4) | Gaurav | Group by layer, don't read bullets — Gaurav takes this since it leans into CI/CD & infra |
| 4:00–5:00 | Switch to live demo machine, login | Vigna | Login as `trader@db.com`, show 200 OK + JWT in DevTools |
| 5:00–6:30 | Post a trade via UI | Vigna | Show the request in the Network tab |
| 6:30–8:00 | Switch to Kafdrop → `trade-events` (localhost:9000) | Nandini | Show the message body land on the topic |
| 8:00–9:30 | Switch to Grafana → request-rate panel (localhost:3000) | Nandini | Point at the panel ticking up |
| 9:30–11:00 | Backend logs + Postgres audit row | Nandini | One pre-staged `psql` command against `audit_log` |
| 11:00–12:00 | Switch back to slides | Avani | "That was the live flow. Now the code." |
| 12:00–13:30 | Code walkthrough — `TradeController` (backend/controller) | Vigna | Owns this file, explains it in own words |
| 13:30–15:00 | Code walkthrough — `ReconConsumer` (backend/kafka) | Nandini | Owns this file |
| 15:00–16:00 | Code walkthrough — `useTradeStream` hook (frontend/hooks) | Shubhang | Owns this file |
| 16:00–17:30 | Learnings slide | Whole team | Avani, Vigna, Nandini, Shubhang, Gaurav — one sentence each, no passing |
| 17:30–21:30 | Q&A | Whole team | Avani routes questions; "we didn't get to that" is a valid answer |

**Hard time-box:** at minute 18, finish the current sentence and move to Q&A regardless of where you are. Overrun bleeds into the next team's slot.

## Fallback notes (one per live step)
- **Kafka slow to report healthy:** narrate over the wait — "while Kafka comes up, let's look at the topic structure in the code"
- **Live trade POST fails:** cut to the screen recording captured during rehearsal
- **Grafana dies:** paste the three saved PNGs into the deck's monitoring slide
- **Any terminal command:** pre-stage command history beforehand so nothing is typed live from memory

## Rehearsal plan (mandatory — two full runs)

**Rehearsal 1 — chaos monkey (target: complete by 15:30)**
Mid-demo, someone unplugs the ethernet / closes a browser tab / hits `Ctrl+C` on a running curl. Goal: practice recovering without breaking flow — "we have a screen recording as backup, here it is."

**Rehearsal 2 — instructor mode (target: complete by 16:15)**
Trainer (or a teammate playing trainer) asks 2–3 hard Q&A-bank questions mid-demo or at the end. Goal: stress-test composure and routing, not perfect answers.


