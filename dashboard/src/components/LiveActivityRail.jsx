import React from 'react';
import { Terminal, Activity, Plus, Edit2, Trash2, ShieldAlert } from 'lucide-react';

function formatRelativeTime(dateString) {
  if (!dateString) return '';
  const now = Date.now();
  const date = new Date(dateString).getTime();
  const diffSec = Math.max(0, Math.floor((now - date) / 1000));

  if (diffSec < 5) return 'just now';
  if (diffSec < 60) return `${diffSec}s ago`;
  const diffMin = Math.floor(diffSec / 60);
  if (diffMin < 60) return `${diffMin}m ago`;
  const diffHour = Math.floor(diffMin / 60);
  if (diffHour < 24) return `${diffHour}h ago`;
  return `${Math.floor(diffHour / 24)}d ago`;
}

function getSourceBadge(source) {
  switch (source) {
    case 'MYSQL_CLI':
      return (
        <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-mono font-bold bg-amber-500/15 text-warn border border-warn/30">
          <Terminal className="w-2.5 h-2.5" />
          MYSQL_CLI
        </span>
      );
    case 'TRACKER':
      return (
        <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-mono font-bold bg-emerald-500/15 text-accent border border-accent/30">
          <Activity className="w-2.5 h-2.5" />
          TRACKER
        </span>
      );
    case 'API':
    default:
      return (
        <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-mono font-medium bg-line/60 text-content-muted border border-line">
          API
        </span>
      );
  }
}

function getActionIcon(action) {
  switch (action) {
    case 'INSERT':
      return <Plus className="w-3 h-3 text-accent" />;
    case 'UPDATE':
      return <Edit2 className="w-3 h-3 text-sky-400" />;
    case 'DELETE':
      return <Trash2 className="w-3 h-3 text-danger" />;
    default:
      return null;
  }
}

export default function LiveActivityRail({ activities = [] }) {
  return (
    <aside className="w-full lg:w-80 flex-shrink-0 border-l border-line bg-panel flex flex-col h-[calc(100dvh-3.5rem)] sticky top-14">
      {/* Header */}
      <div className="p-3.5 border-b border-line flex items-center justify-between bg-panel/60">
        <div className="flex items-center gap-2">
          <span className="w-2 h-2 rounded-full bg-accent animate-pulse" />
          <h2 className="text-xs font-mono uppercase tracking-wider font-semibold text-content">
            Live Activity Rail
          </h2>
        </div>
        <span className="text-[11px] font-mono text-content-muted">
          {activities.length} events
        </span>
      </div>

      {/* Feed List */}
      <div className="flex-1 overflow-y-auto divide-y divide-line/40 p-2 space-y-1.5">
        {activities.length === 0 ? (
          <div className="p-6 text-center text-xs font-mono text-content-muted">
            Waiting for database or tracker events...
          </div>
        ) : (
          activities.map((item) => (
            <div
              key={item.id}
              className="p-2.5 rounded bg-bg/50 border border-line/60 hover:border-line transition-colors animate-slide-down flex flex-col gap-1.5 text-xs"
            >
              <div className="flex items-center justify-between gap-2">
                <div className="flex items-center gap-1.5">
                  <span className="p-1 rounded bg-panel border border-line">
                    {getActionIcon(item.action)}
                  </span>
                  {getSourceBadge(item.source)}
                </div>
                <span className="text-[10px] font-mono text-content-muted whitespace-nowrap">
                  {formatRelativeTime(item.createdAt)}
                </span>
              </div>

              <p className="text-xs text-content/90 font-mono leading-relaxed break-words">
                {item.summary}
              </p>
            </div>
          ))
        )}
      </div>
    </aside>
  );
}

