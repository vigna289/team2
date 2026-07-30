// File: static-dashboard/js/theme.js
// TICKET-ADV100 — theme toggle button handler.
//
// NOTE: the FOUC-avoidance logic (reading localStorage and setting
// data-theme on <html> BEFORE the stylesheet loads) lives in an inline
// <script> in dashboard.html's <head>, ahead of the <link rel="stylesheet">
// tag. This file only wires the toggle button's click behaviour — it runs
// after DOMContentLoaded, by which point the correct theme is already
// painted with zero flash.
(function () {
  document.addEventListener('DOMContentLoaded', () => {
    const btn = document.getElementById('theme-toggle');
    if (!btn) return;

    // Reflect the already-applied theme in aria-pressed on load.
    const current = document.documentElement.getAttribute('data-theme') || 'light';
    btn.setAttribute('aria-pressed', String(current === 'dark'));

    btn.addEventListener('click', () => {
      const next = document.documentElement.getAttribute('data-theme') === 'light' ? 'dark' : 'light';
      document.documentElement.setAttribute('data-theme', next);
      localStorage.setItem('reconx-theme', next);
      btn.setAttribute('aria-pressed', String(next === 'dark'));
    });
  });
})();
