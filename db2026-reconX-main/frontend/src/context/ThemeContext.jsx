// frontend/src/context/ThemeContext.jsx
// TICKET-ADV124 — Theme context. React owns the flip (one string of
// state); CSS owns the actual colour values via [data-theme="dark"]
// selectors that already exist from Day 7. Seeded from localStorage,
// falling back to the OS-level prefers-color-scheme on first visit.
import React, { createContext, useContext, useEffect, useState } from 'react';

const STORAGE_KEY = 'reconx-theme';

const ThemeContext = createContext(null);

function initialTheme() {
  if (typeof window === 'undefined') return 'light';

  const stored = window.localStorage.getItem(STORAGE_KEY);
  if (stored === 'light' || stored === 'dark') return stored;

  const prefersDark = window.matchMedia &&
    window.matchMedia('(prefers-color-scheme: dark)').matches;
  return prefersDark ? 'dark' : 'light';
}

export function ThemeProvider({ children }) {
  const [theme, setTheme] = useState(initialTheme);

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    window.localStorage.setItem(STORAGE_KEY, theme);
  }, [theme]);

  const toggle = () => setTheme((t) => (t === 'light' ? 'dark' : 'light'));

  return (
    <ThemeContext.Provider value={{ theme, setTheme, toggle }}>
      {children}
    </ThemeContext.Provider>
  );
}

export function useTheme() {
  const ctx = useContext(ThemeContext);
  if (ctx === null) {
    throw new Error('useTheme() must be used inside a <ThemeProvider>');
  }
  return ctx;
}
