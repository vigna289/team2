-- ============================================================================
-- TICKET-ADV010 — VWAP per instrument per day (window function)
-- ============================================================================
SELECT DISTINCT
    t.instrument_id,
    t.trade_date,
    SUM(t.price * t.quantity) OVER (PARTITION BY t.instrument_id, t.trade_date)
        / NULLIF(SUM(t.quantity) OVER (PARTITION BY t.instrument_id, t.trade_date), 0)
            AS vwap
FROM trades t
WHERE t.deleted_at IS NULL
  AND t.asset_class = 'EQUITY'
ORDER BY t.trade_date DESC, t.instrument_id;


-- ============================================================================
-- TICKET-ADV011 — Recursive CTE: trade lifecycle (execution -> settlement
--                -> recon_break -> resolution)
-- ============================================================================
WITH RECURSIVE trade_lifecycle AS (
    -- anchor: every trade in its execution state
    SELECT
        t.id           AS trade_id,
        t.trade_ref,
        1              AS step,
        'EXECUTED'     AS state,
        t.created_at   AS at_ts,
        NULL::text     AS detail
    FROM trades t
    WHERE t.deleted_at IS NULL

    UNION ALL

    -- recursive: each subsequent state derived from the previous step
    SELECT
        tl.trade_id,
        tl.trade_ref,
        tl.step + 1,
        CASE tl.step
            WHEN 1 THEN 'CONFIRMED'
            WHEN 2 THEN 'SETTLED'
            WHEN 3 THEN 'RECONCILED'
        END                                          AS state,
        s.settlement_date::timestamp                  AS at_ts,
        s.status                                      AS detail
    FROM trade_lifecycle tl
    JOIN settlements s ON s.trade_id = tl.trade_id
    WHERE tl.step < 4
)
SELECT * FROM trade_lifecycle
ORDER BY trade_id, step;


-- ============================================================================
-- ADV008 — REFRESH the daily-summary materialised view (concurrent so it can
--         run while the dashboard is reading it)
-- ============================================================================
REFRESH MATERIALIZED VIEW CONCURRENTLY mv_daily_recon_summary;


-- ============================================================================
-- ADV009 — JSONB lookup: which instruments have sector = 'Banking'?
-- ============================================================================
SELECT id, symbol, metadata
FROM instruments
WHERE metadata @> '{"sector":"Banking"}'::jsonb;
-- ============================================================================
-- TICKET-ADV010 — VWAP per instrument per day (window function)
--
-- Returns one row per trade (no GROUP BY collapse) with the trade's own
-- quantity/price/notional, plus a vwap column computed via a window
-- function partitioned by (instrument_id, trade_date). Every trade on the
-- same instrument/day shares the same vwap value.
-- ============================================================================

SELECT
    t.trade_ref,
    t.trade_date,
    i.symbol,
    t.quantity,
    t.price,
    t.quantity * t.price AS notional,
    SUM(t.price * t.quantity) OVER (PARTITION BY t.instrument_id, t.trade_date)
        / NULLIF(SUM(t.quantity) OVER (PARTITION BY t.instrument_id, t.trade_date), 0)
            AS vwap,
    ROW_NUMBER() OVER (
        PARTITION BY t.instrument_id, t.trade_date
        ORDER BY t.created_at
    ) AS intraday_seq,
    SUM(t.quantity) OVER (
        PARTITION BY t.instrument_id, t.trade_date
        ORDER BY t.created_at
        ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
    ) AS cumulative_quantity
FROM trades t
JOIN instruments i ON i.id = t.instrument_id
WHERE t.deleted_at IS NULL
ORDER BY t.trade_date DESC, i.symbol, intraday_seq;

-- ============================================================================
-- Sanity check — collapsed to one row per (instrument, day), used to confirm
-- every trade in the partition shares the same vwap value.
-- ============================================================================
SELECT DISTINCT
    t.instrument_id,
    t.trade_date,
    SUM(t.price * t.quantity) OVER (PARTITION BY t.instrument_id, t.trade_date)
        / NULLIF(SUM(t.quantity) OVER (PARTITION BY t.instrument_id, t.trade_date), 0)
            AS vwap
FROM trades t
WHERE t.deleted_at IS NULL
  AND t.asset_class = 'EQUITY'
ORDER BY t.trade_date DESC, t.instrument_id;
-- ============================================================================
-- TICKET-ADV011 — Recursive CTE: trade lifecycle rollup
--
-- Walks each trade through its lifecycle stages
-- (EXECUTED -> CONFIRMED -> SETTLED -> RECONCILED) and emits one row per
-- stage per trade. Base case seeds every trade as EXECUTED; the recursive
-- step joins settlements to derive each subsequent stage. `WHERE tl.step < 4`
-- is the termination guard — Postgres also caps recursion at 100 by default
-- as a hard backstop.
-- ============================================================================

WITH RECURSIVE trade_lifecycle AS (
    -- anchor: every trade in its execution state
    SELECT
        t.id           AS trade_id,
        t.trade_ref,
        1              AS step,
        'EXECUTED'     AS state,
        t.created_at   AS at_ts,
        NULL::text     AS detail
    FROM trades t
    WHERE t.deleted_at IS NULL

    UNION ALL

    -- recursive: each subsequent state derived from the previous step
    SELECT
        tl.trade_id,
        tl.trade_ref,
        tl.step + 1,
        CASE tl.step
            WHEN 1 THEN 'CONFIRMED'
            WHEN 2 THEN 'SETTLED'
            WHEN 3 THEN 'RECONCILED'
        END                                          AS state,
        s.settlement_date::timestamp                  AS at_ts,
        s.status                                      AS detail
    FROM trade_lifecycle tl
    JOIN settlements s ON s.trade_id = tl.trade_id
    WHERE tl.step < 4
)
SELECT * FROM trade_lifecycle
ORDER BY trade_id, step;

-- ============================================================================
-- Single-trade check — swap in a real trade_ref to confirm the lifecycle
-- reads top-to-bottom and terminates in <= 4 rows.
-- ============================================================================
-- WITH RECURSIVE trade_lifecycle AS ( ... same CTE as above ... )
-- SELECT * FROM trade_lifecycle WHERE trade_ref = 'TRD-000001' ORDER BY step;
