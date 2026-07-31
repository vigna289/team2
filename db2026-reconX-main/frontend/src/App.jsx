// frontend/src/App.jsx
// TICKET-ADV122 — React.lazy + Suspense route-based code splitting.
// The <Suspense> boundary sits INSIDE the layout (below the header/nav)
// so the layout stays mounted during navigation and only the page
// content shows the skeleton — wrapping the whole App would flash the
// header on every navigation; wrapping each <Route> individually would
// duplicate the boundary needlessly.
import React, { Suspense, lazy } from 'react';
import { Routes, Route, Link, Navigate } from 'react-router-dom';
import { withErrorBoundary } from '@components/withErrorBoundary.jsx';
import { PageSkeleton } from '@components/PageSkeleton.jsx';
import { useAuth } from '@context/AuthContext.jsx';
import { useTheme } from '@context/ThemeContext.jsx';

const Dashboard = lazy(() => import('@pages/Dashboard.jsx'));
const Trades    = lazy(() => import('@pages/Trades.jsx'));
const AddTrade  = lazy(() => import('@pages/AddTrade.jsx'));
const Login     = lazy(() => import('@pages/Login.jsx'));

function App() {
  const { user, logout } = useAuth();
  const { theme, toggle } = useTheme();

  return (
    <div className="layout">
      <header className="layout__header">
        <h1>ReconX</h1>
        <nav className="layout__nav">
          <Link to="/">Dashboard</Link>
          <Link to="/trades">Trades</Link>
          <Link to="/trades/new">Add trade</Link>
        </nav>
        <button onClick={toggle} aria-label="Toggle theme">
          {theme === 'light' ? '🌙' : '☀️'}
        </button>
        {user && <button onClick={logout}>Logout</button>}
      </header>

      <main className="layout__main">
        <Suspense fallback={<PageSkeleton />}>
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route path="/" element={<Dashboard />} />
            <Route path="/trades" element={<Trades />} />
            <Route path="/trades/new" element={<AddTrade />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </Suspense>
      </main>
    </div>
  );
}

export default withErrorBoundary(App);
