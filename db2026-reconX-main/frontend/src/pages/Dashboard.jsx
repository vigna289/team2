// frontend/src/pages/Dashboard.jsx
// TICKET-ADV120 — useMemo for portfolio value and status counts, keyed
// on [trades] so they recompute only when the trades array actually
// changes (a new SSE arrival), not on unrelated state flips (theme
// toggle, side panel open/close).
//
// The optional `trades` prop lets TICKET-ADV125's RTL test render this
// component with seeded data, bypassing useTradeStream()'s live
// EventSource entirely — no backend/mocking needed for the test.
import React, { useEffect, useMemo, useState } from 'react';
import { withAuth } from '@components/withAuth.jsx';
import { api } from '@services/apiService.js';

function StatCard({ label, value }) {
  return (
    <article className="stat-card">
      <h3>{label}</h3>
      <p>{value}</p>
    </article>
  );
}

function Dashboard({ trades: tradesProp }) {
  const [trades, setTrades] = useState(tradesProp ?? []);
  const [isConnected, setIsConnected] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (tradesProp) {
      return;
    }

    async function loadTrades() {
      try {
        const response = await api.listTrades();
        setTrades(response.content ?? []);
        setIsConnected(true);
      } catch (err) {
        setError(err.message);
        setIsConnected(false);
      }
    }

    loadTrades();
  }, [tradesProp]);

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
      {error && <div role="alert" className="form-error">{error}</div>}
      <div className="stat-grid">
        <StatCard label="Portfolio value (USD)" value={portfolioValue.toLocaleString()} />
        <StatCard label="Trades loaded" value={trades.length} />
        <StatCard label="Matched trades" value={stats.matched} />
        <StatCard label="Unmatched trades" value={stats.unmatched} />
        <StatCard label="Disputed" value={stats.disputed} />
      </div>
      <div role="status" aria-live="polite">
        API: {isConnected ? 'connected' : 'disconnected'}
      </div>
    </section>
  );
}

export default withAuth(Dashboard);
