// TICKET-ADV118 — useInfiniteScroll: invokes loadMore() when sentinel is visible.

import { useEffect, useRef } from 'react';

export function useInfiniteScroll(loadMore) {
  const sentinelRef = useRef(null);
  const loadMoreRef = useRef(loadMore);

  // Keep latest loadMore without recreating observer
  useEffect(() => {
    loadMoreRef.current = loadMore;
  }, [loadMore]);

  // Create observer once
  useEffect(() => {
    const sentinel = sentinelRef.current;

    if (!sentinel) return;

    const observer = new IntersectionObserver((entries) => {
      if (entries[0].isIntersecting) {
        loadMoreRef.current();
      }
    });

    observer.observe(sentinel);

    return () => {
      observer.disconnect();
    };
  }, []);

  return sentinelRef;
}