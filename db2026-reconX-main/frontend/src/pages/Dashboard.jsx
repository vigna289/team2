// frontend/src/pages/Dashboard.jsx
// TICKET-ADV120 — useMemo for portfolio value and status counts, keyed
// on [trades] so they recompute only when the trades array actually
// changes (a new SSE arrival), not on unrelated state flips (theme
// toggle, side panel open/close).
//
// The optional `trades` prop lets TICKET-ADV125's RTL test render this
// component with seeded data, bypassing useTradeStream()'s live
// EventSource entirely — no backend/mocking needed for the test.
import React, { useMemo } from 'react';
import { withAuth } from '@components/withAuth.jsx';
import { useTradeStream } from '@hooks/useTradeStream.js';

function StatCard({ label, value }) {
  return (
    <article className="stat-card">
      <h3>{label}</h3>
      <p>{value}</p>
    </article>
  );
}

function Dashboard({ trades: tradesProp }) {
  const stream = useTradeStream();
  const trades = tradesProp ?? stream.trades;
  const isConnected = tradesProp ? true : stream.isConnected;

  // Worth memoising: O(n) reduce over potentially hundreds of trades,
  // recomputed every time a new trade streams in — but not on every
  // unrelated render (e.g. theme toggle re-rendering the whole tree).
  const portfolioValue = useMemo(
    () => trades.reduce((sum, t) => sum + (t.quantity * t.price || 0), 0),
    [trades]
  );

  // Worth memoising: same reasoning — three status filters over the
  // same array, only needs to redo the work when trades itself changes.
  const stats = useMemo(() => {
    const matched = trades.filter((t) => t.status === 'MATCHED').length;
    const unmatched = trades.filter((t) => t.status === 'UNMATCHED').length;
    const disputed = trades.filter((t) => t.status === 'DISPUTED').length;
    return { matched, unmatched, disputed };
  }, [trades]);

  return (
    <section>
      <h2>Dashboard</h2>
      <div className="stat-grid">
        <StatCard label="Portfolio value (USD)" value={portfolioValue.toLocaleString()} />
        <StatCard label="Trades streamed" value={trades.length} />
        <StatCard label="Matched trades" value={stats.matched} />
        <StatCard label="Unmatched trades" value={stats.unmatched} />
        <StatCard label="Disputed" value={stats.disputed} />
      </div>
      <div role="status" aria-live="polite">
        SSE: {isConnected ? 'connected' : 'disconnected'}
      </div>
    </section>
  );
}

export default withAuth(Dashboard);
