import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Login from './pages/Login';
import BugsPage from './pages/BugsPage';
import BugDetailPage from './pages/BugDetailPage';
import { getToken } from './api/client';

function ProtectedRoute({ children }) {
  const token = getToken();
  if (!token) {
    return <Navigate to="/login" replace />;
  }
  return children;
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        
        <Route
          path="/bugs"
          element={
            <ProtectedRoute>
              <BugsPage />
            </ProtectedRoute>
          }
        />
        
        <Route
          path="/bugs/:id"
          element={
            <ProtectedRoute>
              <BugDetailPage />
            </ProtectedRoute>
          }
        />

        <Route
          path="/analytics"
          element={
            <ProtectedRoute>
              <div className="min-h-dvh flex items-center justify-center p-6 bg-bg text-content font-mono">
                <div className="p-8 rounded bg-panel border border-line text-center space-y-3">
                  <h1 className="text-base font-bold text-accent">Analytics Overview</h1>
                  <p className="text-xs text-content-muted">Full aggregated analytics & 14-day error charts will be built in Phase 6.</p>
                  <a href="/bugs" className="inline-block px-3 py-1.5 rounded bg-line text-xs hover:text-accent">
                    Return to Bugs
                  </a>
                </div>
              </div>
            </ProtectedRoute>
          }
        />

        <Route path="*" element={<Navigate to="/bugs" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
