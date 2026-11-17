import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Login from './pages/Login';
import BugsPage from './pages/BugsPage';
import BugDetailPage from './pages/BugDetailPage';
import AnalyticsPage from './pages/AnalyticsPage';
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
              <AnalyticsPage />
            </ProtectedRoute>
          }
        />

        <Route path="*" element={<Navigate to="/bugs" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
