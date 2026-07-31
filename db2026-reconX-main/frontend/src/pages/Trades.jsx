// frontend/src/pages/Trades.jsx
// TICKET-ADV121 — useCallback on the handler passed into <TradeRow />.
// ADV119's memo equality includes prev.onClick === next.onClick; without
// useCallback, every parent render rebuilds a new arrow function and the
// memo silently fails. This is what makes ADV119's win real.
import React, { useCallback, useState } from 'react';
import { withAuth } from '@components/withAuth.jsx';
import { TradeRow } from '@components/TradeRow.jsx';
import DataTable from '@components/DataTable.jsx';

import { useDebouncedSearch } from '@hooks/useDebouncedSearch.js';

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
  const debouncedFilter = useDebouncedSearch(statusFilter, 300);

  // Reference-stable across renders — the onClick prop on <TradeRow>
  // never changes identity, so ADV119's memo equality check passes.
  const handleSelect = useCallback((id) => setSelectedId(id), []);

  const filtered = debouncedFilter
    ? trades.filter((t) => t.status.toLowerCase().includes(debouncedFilter.toLowerCase()))
    : trades;

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
          renderRow={(t) => <TradeRow key={t.id} trade={t} onClick={handleSelect} />}
        />
        <DataTable.Pagination />
      </DataTable>

      {selectedId && <p>Selected trade id: {selectedId}</p>}
    </section>
  );
}

export default withAuth(Trades);
