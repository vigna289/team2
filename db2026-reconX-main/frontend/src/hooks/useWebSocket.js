// TICKET-ADV115 — useWebSocket(url) with auto-reconnect (exp backoff up to 5 tries).

import { useEffect, useRef, useState } from 'react';

export function useWebSocket(
  url,
  { reconnect = true, maxRetries = 5 } = {}
) {
  const [data, setData] = useState(null);
  const [status, setStatus] = useState('connecting');

  const socketRef = useRef(null);
  const retryRef = useRef(0);
  const timerRef = useRef(null);

  useEffect(() => {
    let mounted = true;

    const connect = () => {
      if (!mounted) return;

      setStatus('connecting');

      const socket = new WebSocket(url);
      socketRef.current = socket;

      socket.onopen = () => {
        retryRef.current = 0;
        setStatus('open');
      };

      socket.onmessage = (event) => {
        try {
          setData(JSON.parse(event.data));
        } catch {
          setData(event.data);
        }
      };

      socket.onerror = () => {
        setStatus('error');
      };

      socket.onclose = () => {
        setStatus('closed');

        if (
          reconnect &&
          retryRef.current < maxRetries &&
          mounted
        ) {
          const attempt = retryRef.current;

          const delay = Math.min(
            500 * 2 ** attempt,
            30000
          );

          retryRef.current += 1;

          timerRef.current = setTimeout(() => {
            connect();
          }, delay);
        }
      };
    };

    connect();

    return () => {
      mounted = false;

      if (timerRef.current) {
        clearTimeout(timerRef.current);
      }

      if (socketRef.current) {
        socketRef.current.close();
      }
    };
  }, [url, reconnect, maxRetries]);


  const send = (payload) => {
    const socket = socketRef.current;

    if (
      socket &&
      socket.readyState === WebSocket.OPEN
    ) {
      socket.send(
        typeof payload === 'string'
          ? payload
          : JSON.stringify(payload)
      );
    }
  };


  return { data, status, send };
}