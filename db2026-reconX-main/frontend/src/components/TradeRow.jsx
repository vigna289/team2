// frontend/src/components/TradeRow.jsx
// TICKET-ADV119 — React.memo with a custom equality check so rows only
// re-render when the fields they actually display change (id, status,
// price) or when the onClick handler's identity changes. The onClick
// identity check will keep failing until TICKET-ADV121 wraps the
// parent's handler in useCallback — that's expected until then.
import React from 'react';

function TradeRowImpl({ trade, onClick }) {
  return (
    <tr onClick={() => onClick(trade.id)}>
      <td>{trade.tradeRef}</td>
      <td>{trade.instrument}</td>
      <td>{trade.quantity}</td>
      <td>{trade.price}</td>
      <td>
        <span className={`status-pill ${trade.status.toLowerCase()}`}>
          {trade.status}
        </span>
      </td>
    </tr>
  );
}

// Custom equality — only the fields this row actually reads.
// Returning true means "props are equal, skip the render".
function areEqual(prev, next) {
  return (
    prev.trade.id === next.trade.id &&
    prev.trade.status === next.trade.status &&
    prev.trade.price === next.trade.price &&
    prev.onClick === next.onClick
  );
}

export const TradeRow = React.memo(TradeRowImpl, areEqual);
