// File: static-dashboard/js/trades.js
(function () {
    const table = document.getElementById('trades-table');
    const tbody = document.getElementById('trades-tbody');
    let rows = [];
  
    // ---------- SORT ----------
    table.querySelectorAll('thead th').forEach(th => {
      th.addEventListener('click', (e) => {
        if (e.target.classList.contains('resize-handle')) return;
  
        const col = th.dataset.col;
        const type = th.dataset.type || 'string';
        const dir = th.getAttribute('aria-sort') === 'ascending'
          ? 'descending'
          : 'ascending';
  
        table.querySelectorAll('thead th').forEach(o => o.removeAttribute('aria-sort'));
        th.setAttribute('aria-sort', dir);
  
        const mult = dir === 'ascending' ? 1 : -1;
  
        rows.sort((a, b) => {
          const av = a[col], bv = b[col];
          if (type === 'number') return (Number(av) - Number(bv)) * mult;
          return String(av).localeCompare(String(bv)) * mult;
        });
  
        renderRows();
      });
    });
  
    // ---------- RESIZE ----------
    table.querySelectorAll('.resize-handle').forEach(handle => {
      handle.addEventListener('mousedown', (e) => {
        e.preventDefault();
        const th = handle.closest('th');
        const startX = e.clientX;
        const startWidth = th.offsetWidth;
  
        function onMove(ev) {
          th.style.width = (startWidth + ev.clientX - startX) + 'px';
        }
  
        function onUp() {
          document.removeEventListener('mousemove', onMove);
          document.removeEventListener('mouseup', onUp);
        }
  
        document.addEventListener('mousemove', onMove);
        document.addEventListener('mouseup', onUp);
      });
    });
  
    // ---------- RENDER ----------
    function renderRows() {
      tbody.innerHTML = rows.map(r => `
        <tr>
          <td>${r.tradeRef}</td>
          <td>${r.symbol}</td>
          <td>${r.quantity}</td>
          <td>${r.price}</td>
          <td>${r.status}</td>
        </tr>
      `).join('');
    }
  
    // ---------- LOAD DATA ----------
    fetch('/api/v1/trades?size=200')
      .then(r => r.json())
      .then(data => {
        rows = data.content || data;
        renderRows();
      });
  })();
  