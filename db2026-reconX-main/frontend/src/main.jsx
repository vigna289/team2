// frontend/src/main.jsx
// TICKET-ADV124 — ThemeProvider lifted above AuthProvider and BrowserRouter
// so the withErrorBoundary fallback (which wraps <App/> further down) can
// still read theme even if something inside App throws.
//
// NOTE: per the Day 8 solved-files doc, this file should already exist
// from TICKET-ADV111 (Vite path aliases) with AuthProvider + BrowserRouter
// wired in. If so, don't blindly overwrite — just make sure ThemeProvider
// wraps everything else, matching the nesting order below.
import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import App from './App.jsx';
import { ThemeProvider } from '@context/ThemeContext.jsx';
import { AuthProvider } from '@context/AuthContext.jsx';
import './styles/global.css';

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <ThemeProvider>
      <AuthProvider>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </AuthProvider>
    </ThemeProvider>
  </React.StrictMode>
);
