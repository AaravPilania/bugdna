import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api, setToken } from '../api/client';
import { Shield, ArrowRight, Lock, User } from 'lucide-react';

export default function Login() {
  const navigate = useNavigate();
  const [username, setUsername] = useState('dev');
  const [password, setPassword] = useState('password123');
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      const response = await api.auth.login(username, password);
      if (response?.token) {
        setToken(response.token);
        sessionStorage.setItem('bugdna_user', response.displayName || username);
        navigate('/bugs');
      } else {
        setError('Authentication did not return a valid session token.');
      }
    } catch (err) {
      setError(err.message || 'Login failed. Please check credentials.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-dvh flex items-center justify-center p-4 bg-bg text-content relative overflow-hidden">
      {/* Background Genome Grid effect */}
      <div className="absolute inset-0 bg-[radial-gradient(#22262C_1px,transparent_1px)] [background-size:20px_20px] opacity-30 pointer-events-none" />

      <div className="w-full max-w-sm rounded-lg bg-panel border border-line shadow-2xl p-6 md:p-8 flex flex-col gap-6 relative z-10 animate-slide-down">
        {/* Brand */}
        <div className="flex flex-col items-center text-center gap-2">
          <div className="w-10 h-10 rounded-md bg-bg border border-line flex items-center justify-center shadow-inner" aria-hidden="true">
            <svg width="20" height="20" viewBox="0 0 16 16" fill="none">
              <rect x="2" y="2" width="3" height="12" rx="1" fill="#C8F169" />
              <rect x="6.5" y="4" width="3" height="8" rx="1" fill="#6495ED" />
              <rect x="11" y="2" width="3" height="12" rx="1" fill="#F2B544" />
            </svg>
          </div>
          <h1 className="font-mono text-xl font-bold tracking-tight text-content">
            BugDNA Console
          </h1>
          <p className="text-xs font-mono text-content-muted">
            Developer session authentication
          </p>
        </div>

        {error && (
          <div className="p-3 rounded bg-danger/10 border border-danger/30 text-danger text-xs font-mono">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <label
              htmlFor="username"
              className="text-[11px] font-mono uppercase tracking-wider text-content-muted"
            >
              Developer Username
            </label>
            <div className="relative">
              <User className="w-4 h-4 text-content-muted absolute left-3 top-2.5 pointer-events-none" />
              <input
                id="username"
                type="text"
                required
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="dev"
                className="w-full pl-9 pr-3 py-2 rounded bg-bg border border-line text-xs font-mono text-content placeholder:text-content-muted/50 focus:border-accent focus:ring-1 focus:ring-accent focus:outline-none transition-colors"
              />
            </div>
          </div>

          <div className="flex flex-col gap-1.5">
            <label
              htmlFor="password"
              className="text-[11px] font-mono uppercase tracking-wider text-content-muted"
            >
              Password
            </label>
            <div className="relative">
              <Lock className="w-4 h-4 text-content-muted absolute left-3 top-2.5 pointer-events-none" />
              <input
                id="password"
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                className="w-full pl-9 pr-3 py-2 rounded bg-bg border border-line text-xs font-mono text-content placeholder:text-content-muted/50 focus:border-accent focus:ring-1 focus:ring-accent focus:outline-none transition-colors"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="mt-2 w-full py-2.5 px-4 rounded bg-accent text-bg font-mono font-semibold text-xs flex items-center justify-center gap-2 hover:bg-accent/90 focus:outline-none focus:ring-2 focus:ring-accent focus:ring-offset-2 focus:ring-offset-bg transition-colors disabled:opacity-50"
          >
            {loading ? (
              'Authenticating...'
            ) : (
              <>
                Sign in to Dashboard <ArrowRight className="w-3.5 h-3.5" />
              </>
            )}
          </button>
        </form>

        <div className="pt-4 border-t border-line text-center text-[11px] font-mono text-content-muted flex items-center justify-center gap-1.5">
          <Shield className="w-3.5 h-3.5 text-accent" />
          <span>Local v1 Auth · In-memory Bearer Token</span>
        </div>
      </div>
    </div>
  );
}

