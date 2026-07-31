// frontend/src/components/PageSkeleton.jsx
// TICKET-ADV122 — Suspense fallback. A real skeleton (not a spinner, not
// null) so first paint during a chunk download doesn't look broken.
import React from 'react';

export function PageSkeleton() {
  return (
    <div className="page-skeleton" aria-busy="true" aria-label="Loading page">
      <div className="page-skeleton__block page-skeleton__block--title" />
      <div className="page-skeleton__block page-skeleton__block--card" />
      <div className="page-skeleton__block page-skeleton__block--card" />
      <div className="page-skeleton__block page-skeleton__block--card" />
    </div>
  );
}
