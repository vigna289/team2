// TICKET-ADV116 — useTradeStream() — SSE subscription returning live trades.

import { useEffect, useState } from 'react';

export function useTradeStream(url = '/api/v1/trades/stream') {
  const [trades, setTrades] = useState([]);
  const [isConnected, setConnected] = useState(false);

  useEffect(() => {
    const source = new EventSource(url);

    source.onopen = () => {
      setConnected(true);
    };

    source.onmessage = (event) => {
      try {
        const trade = JSON.parse(event.data);

        setTrades((prev) => [
          trade,
          ...prev,
        ].slice(0, 200));

      } catch (error) {
        console.error('Invalid SSE message', error);
      }
    };

    source.onerror = () => {
      setConnected(false);
    };

    return () => {
      source.close();
    };

  }, [url]);

  return { trades, isConnected };
}
