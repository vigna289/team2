// frontend/src/pages/Trades.jsx
// TICKET-ADV121 — useCallback on the handler passed into <TradeRow />.
// ADV119's memo equality includes prev.onClick === next.onClick; without
// useCallback, every parent render rebuilds a new arrow function and the
// memo silently fails. This is what makes ADV119's win real.
import React, { useCallback, useEffect, useState } from 'react';
import { withAuth } from '@components/withAuth.jsx';
import { TradeRow } from '@components/TradeRow.jsx';
import DataTable from '@components/DataTable.jsx';

import { useDebouncedSearch } from '@hooks/useDebouncedSearch.js';
import { api } from '@services/apiService.js';

const columns = [
  { key: 'tradeRef', label: 'Trade Ref' },
  { key: 'instrument', label: 'Instrument' },
  { key: 'quantity', label: 'Quantity' },
  { key: 'price', label: 'Price' },
  { key: 'status', label: 'Status' },
];

function Trades({ trades = [] }) {
  const [selectedId, setSelectedId] = useState(null);
  const [statusFilter, setStatusFilter] = useState('');
  const [loadedTrades, setLoadedTrades] = useState(trades);
  const [error, setError] = useState(null);
  const debouncedFilter = useDebouncedSearch(statusFilter, 300);

  useEffect(() => {
    if (trades.length > 0) {
      setLoadedTrades(trades);
      return;
    }

    async function loadTrades() {
      try {
        const response = await api.listTrades();
        setLoadedTrades(response.content ?? []);
      } catch (err) {
        setError(err.message);
      }
    }

    loadTrades();
  }, [trades]);

  // Reference-stable across renders — the onClick prop on <TradeRow>
  // never changes identity, so ADV119's memo equality check passes.
  const handleSelect = useCallback((id) => setSelectedId(id), []);

  const filtered = debouncedFilter
    ? loadedTrades.filter((t) => t.status.toLowerCase().includes(debouncedFilter.toLowerCase()))
    : loadedTrades;

  return (
    <section>
      <h2>Trades</h2>
      <label>
        Filter by status
        <input
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          placeholder="e.g. MATCHED"
        />
      </label>

      <DataTable data={filtered}>
        <DataTable.Header columns={columns} />
        <DataTable.Body
          rows={filtered}
          renderRow={(t) => <TradeRow key={t.id} trade={t} onClick={handleSelect} />}
        />
        <DataTable.Pagination />
      </DataTable>

      {error && <div role="alert" className="form-error">{error}</div>}
      {selectedId && <p>Selected trade id: {selectedId}</p>}
    </section>
  );
}

export default withAuth(Trades);
