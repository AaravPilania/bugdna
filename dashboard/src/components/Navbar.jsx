import React from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { RefreshCw, LogOut, Terminal, Activity, ShieldCheck } from 'lucide-react';
import { api, removeToken } from '../api/client';

export default function Navbar({ syncStatus, secondsAgo, onForceSync }) {
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = async () => {
    try {
      await api.auth.logout();
    } finally {
      removeToken();
      navigate('/login');
    }
  };

  const isBugs = location.pathname.startsWith('/bugs');
  const isAnalytics = location.pathname.startsWith('/analytics');

  return (
    <header className="h-14 border-b border-line bg-panel/90 backdrop-blur-md sticky top-0 z-40 px-4 md:px-6 flex items-center justify-between">
      {/* Left: Brand & Navigation */}
      <div className="flex items-center gap-8">
        <Link to="/bugs" className="flex items-center gap-2.5 group">
          <div className="w-7 h-7 rounded bg-bg border border-line flex items-center justify-center font-mono font-bold text-accent text-sm group-hover:border-accent transition-colors">
            🧬
          </div>
          <div className="flex flex-col">
            <span className="font-mono font-bold text-base tracking-tight text-content group-hover:text-accent transition-colors">
              BugDNA
            </span>
            <span className="text-[10px] font-mono uppercase tracking-wider text-content-muted leading-none">
              Autonomous Error Genome
            </span>
          </div>
        </Link>

        <nav className="hidden sm:flex items-center gap-1 font-mono text-xs">
          <Link
            to="/bugs"
            className={`px-3 py-1.5 rounded transition-colors ${
              isBugs
                ? 'bg-line/60 text-content font-medium'
                : 'text-content-muted hover:text-content hover:bg-line/30'
            }`}
          >
            Bugs
          </Link>
          <Link
            to="/analytics"
            className={`px-3 py-1.5 rounded transition-colors ${
              isAnalytics
                ? 'bg-line/60 text-content font-medium'
                : 'text-content-muted hover:text-content hover:bg-line/30'
            }`}
          >
            Analytics
          </Link>
        </nav>
      </div>

      {/* Right: Sync Status & Auth */}
      <div className="flex items-center gap-4">
        {/* Sync Indicator */}
        <div className="flex items-center gap-2 px-2.5 py-1 rounded bg-bg/80 border border-line text-xs font-mono">
          <span
            className={`w-2 h-2 rounded-full transition-colors ${
              syncStatus === 'syncing'
                ? 'bg-warn animate-ping'
                : syncStatus === 'error'
                ? 'bg-danger'
                : 'bg-accent shadow-[0_0_6px_rgba(200,241,105,0.8)]'
            }`}
          />
          <span className="text-content-muted">
            {syncStatus === 'syncing'
              ? 'Syncing...'
              : syncStatus === 'error'
              ? 'Sync Disconnected'
              : `Synced ${secondsAgo}s ago`}
          </span>
          <button
            onClick={onForceSync}
            title="Force Poll Server"
            aria-label="Force Poll Server"
            className="p-1 text-content-muted hover:text-accent transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-accent rounded"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${syncStatus === 'syncing' ? 'animate-spin' : ''}`} />
          </button>
        </div>

        {/* User & Logout */}
        <div className="flex items-center gap-2 pl-2 border-l border-line text-xs font-mono">
          <span className="text-content-muted hidden md:inline">dev@bugdna</span>
          <button
            onClick={handleLogout}
            title="Log out"
            aria-label="Log out"
            className="p-1.5 rounded text-content-muted hover:text-danger hover:bg-danger/10 transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-danger"
          >
            <LogOut className="w-4 h-4" />
          </button>
        </div>
      </div>
    </header>
  );
}

