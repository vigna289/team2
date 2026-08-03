// frontend/src/components/TradeRow.jsx
// TICKET-ADV119 — React.memo with a custom equality check so rows only
// re-render when the fields they actually display change (id, status,
// price) or when the onClick handler's identity changes. The onClick
// identity check will keep failing until TICKET-ADV121 wraps the
// parent's handler in useCallback — that's expected until then.
import React from 'react';

function TradeRowImpl({ trade, onClick }) {
  return (
    <div className="trade-row" onClick={() => onClick(trade.id)}>
      <div className="trade-row__cell">{trade.tradeRef}</div>
      <div className="trade-row__cell">{trade.instrument}</div>
      <div className="trade-row__cell">{trade.quantity}</div>
      <div className="trade-row__cell">{trade.price}</div>
      <div className="trade-row__cell">
        <span className={`status-pill ${trade.status.toLowerCase()}`}>
          {trade.status}
        </span>
      </div>
    </div>
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
