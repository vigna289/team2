// File: static-dashboard/js/sse.js
// TICKET-ADV101 — live trade feed area. This is a demo stub with hardcoded
// events so the static page works with no backend running (TICKET-ADV104's
// real EventSource wiring replaces the demoEvents block below — see the
// commented-out real version at the bottom of this file).
// File: static-dashboard/js/sse.js
(function () {
  const FEED_EL = document.getElementById('trade-feed');
  const BADGE_EL = document.getElementById('sse-status');
  if (!FEED_EL || !BADGE_EL) return;

  const STREAM_URL = '/api/v1/trades/stream';
  let sse = null;

  function updateBadge(text, variant) {
    BADGE_EL.textContent = text;
    BADGE_EL.className = 'sse-status ' + variant;
  }

  function connect() {
    sse = new EventSource(STREAM_URL);

    sse.onopen = () => {
      updateBadge('Live', 'sse-status--live');
    };

    sse.onmessage = (event) => {
      try {
        const trade = JSON.parse(event.data);
        prependTradeRow(trade);
      } catch (err) {
        console.error('Bad SSE payload:', err);
      }
    };

    sse.onerror = () => {
      updateBadge('Reconnecting…', 'sse-status--reconnecting');
      // DO NOT reconnect manually — browser handles it.
    };
  }

  window.addEventListener('beforeunload', () => sse?.close());

  // --- ADV105 prepend + animate + cap ---
function escapeHtml(str) {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

const fmtQty = new Intl.NumberFormat('en-US');
const fmtPrice = new Intl.NumberFormat('en-US', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 4
});

function prependTradeRow(trade) {
  const statusMod = 'trade-card--' + trade.status.toLowerCase();

  const row = document.createElement('article');
  row.className = `trade-card ${statusMod} trade-card--new`;

  row.innerHTML = `
    <header class="trade-card__header">
      <strong>${escapeHtml(trade.tradeRef)}</strong>
      <span>[${escapeHtml(trade.status)}]</span>
    </header>
    <div class="trade-card__body">
      <span>${escapeHtml(trade.symbol)}</span>
      <span>Qty: ${fmtQty.format(trade.qty)}</span>
      <span>Price: ${fmtPrice.format(trade.price)}</span>
    </div>
  `;

  FEED_EL.prepend(row);

  // Remove the animation modifier after entrance
  setTimeout(() => row.classList.remove('trade-card--new'), 500);

  // Cap at 50
  while (FEED_EL.children.length > 50) {
    FEED_EL.lastElementChild.remove();
  }
}


  connect();
})();
