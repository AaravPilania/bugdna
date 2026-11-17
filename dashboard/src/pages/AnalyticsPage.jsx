import React, { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import { useSyncState } from '../hooks/useSyncState';
import Navbar from '../components/Navbar';
import { AlertCircle, BarChart3, ChevronRight } from 'lucide-react';

const STATUS_ORDER = ['OPEN', 'IN_PROGRESS', 'REOPENED', 'RESOLVED', 'CLOSED', 'IGNORED'];
const PRIORITY_ORDER = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];

function getPriorityTone(priority) {
  switch (priority) {
    case 'CRITICAL':
      return 'bg-danger';
    case 'HIGH':
      return 'bg-warn';
    case 'MEDIUM':
      return 'bg-content/70';
    default:
      return 'bg-content-muted/50';
  }
}

export default function AnalyticsPage() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchOverview = useCallback(async () => {
    try {
      setError(null);
      const res = await api.analytics.overview();
      setData(res);
    } catch (err) {
      if (err.message !== 'Unauthorized') {
        setError(err.message || 'Failed to load analytics overview.');
      }
    } finally {
      setLoading(false);
    }
  }, []);

  const { syncStatus, secondsAgo, forceSync } = useSyncState({
    onBugsChanged: fetchOverview,
  });

  useEffect(() => {
    fetchOverview();
  }, [fetchOverview]);

  const maxDaily = data?.errorsPerDay
    ? Math.max(1, ...data.errorsPerDay.map((d) => d.count))
    : 1;

  return (
    <div className="min-h-dvh flex flex-col bg-bg text-content">
      <Navbar
        syncStatus={syncStatus}
        secondsAgo={secondsAgo}
        onForceSync={() => {
          fetchOverview();
          forceSync();
        }}
      />

      <main className="flex-1 max-w-6xl w-full mx-auto p-4 md:p-6 flex flex-col gap-6">
        <div className="pb-3 border-b border-line flex flex-col sm:flex-row sm:items-end justify-between gap-2">
          <div>
            <h1 className="text-lg font-mono font-bold tracking-tight text-content flex items-center gap-2 text-balance">
              <BarChart3 className="w-4 h-4 text-accent" />
              <span>Analytics Overview</span>
            </h1>
            <p className="text-xs font-mono text-content-muted mt-0.5">
              Live metrics aggregated from MySQL <code className="text-content">VIEW bug_summary</code> and 14-day error telemetry
            </p>
          </div>
        </div>

        {error ? (
          <div className="p-4 rounded bg-danger/10 border border-danger/30 text-danger text-xs font-mono flex items-center gap-2">
            <AlertCircle className="w-4 h-4 flex-shrink-0" />
            <span>{error}</span>
          </div>
        ) : loading || !data ? (
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 animate-pulse">
            {[1, 2, 3, 4].map((n) => (
              <div key={n} className="h-24 rounded bg-panel border border-line" />
            ))}
          </div>
        ) : (
          <>
            {/* KPI Row */}
            <section className="grid grid-cols-2 lg:grid-cols-4 gap-4 font-mono">
              <div className="p-4 rounded bg-panel border border-line flex flex-col gap-1">
                <span className="text-[11px] uppercase tracking-wider text-content-muted">
                  Total Bugs
                </span>
                <span className="text-2xl font-bold text-content tabular-nums">
                  {data.totalBugs}
                </span>
              </div>

              <div className="p-4 rounded bg-panel border border-line flex flex-col gap-1">
                <span className="text-[11px] uppercase tracking-wider text-content-muted">
                  Total Occurrences
                </span>
                <span className="text-2xl font-bold text-accent tabular-nums">
                  {data.totalOccurrences}
                </span>
              </div>

              <div className="p-4 rounded bg-panel border border-line flex flex-col gap-1">
                <span className="text-[11px] uppercase tracking-wider text-content-muted">
                  Regressions Flagged
                </span>
                <span
                  className={`text-2xl font-bold tabular-nums ${
                    data.regressionCount > 0 ? 'text-danger' : 'text-content'
                  }`}
                >
                  {data.regressionCount}
                </span>
              </div>

              <div className="p-4 rounded bg-panel border border-line flex flex-col gap-1">
                <span className="text-[11px] uppercase tracking-wider text-content-muted">
                  Mean Time to Resolve
                </span>
                <span className="text-2xl font-bold text-content tabular-nums">
                  {data.meanMinutesToResolve != null ? `${data.meanMinutesToResolve}m` : '—'}
                </span>
              </div>
            </section>

            {/* 14-Day Error Volume SVG Bar Chart */}
            <section className="p-4 rounded bg-panel border border-line flex flex-col gap-4 font-mono">
              <div className="flex items-center justify-between border-b border-line pb-2.5">
                <h2 className="text-xs uppercase tracking-wider font-bold text-content-muted">
                  Error Events Per Day (Last 14 Days)
                </h2>
                <span className="text-[11px] text-content-muted tabular-nums">
                  Peak: {maxDaily} event{maxDaily === 1 ? '' : 's'}
                </span>
              </div>

              <div className="w-full overflow-x-auto">
                <svg
                  viewBox="0 0 700 140"
                  role="img"
                  aria-label="Bar chart of error events per day over the last 14 days"
                  className="w-full h-36 min-w-[520px]"
                >
                  {/* Baseline */}
                  <line x1="0" y1="112" x2="700" y2="112" stroke="#22262C" strokeWidth="1" />
                  {data.errorsPerDay.map((d, idx) => {
                    const barHeight = d.count > 0 ? Math.max(6, Math.round((d.count / maxDaily) * 90)) : 2;
                    const x = idx * 50 + 10;
                    const y = 112 - barHeight;
                    const shortLabel = d.date.slice(5); // MM-DD
                    return (
                      <g key={d.date}>
                        <rect
                          x={x}
                          y={y}
                          width="30"
                          height={barHeight}
                          rx="2"
                          fill={d.count > 0 ? '#C8F169' : '#22262C'}
                          opacity={d.count > 0 ? 0.9 : 0.6}
                        >
                          <title>{`${d.date}: ${d.count} error event(s)`}</title>
                        </rect>
                        {d.count > 0 && (
                          <text
                            x={x + 15}
                            y={y - 5}
                            textAnchor="middle"
                            fill="#ECEAE4"
                            fontSize="10"
                            fontFamily="Geist Mono, monospace"
                          >
                            {d.count}
                          </text>
                        )}
                        <text
                          x={x + 15}
                          y="130"
                          textAnchor="middle"
                          fill="#8B9097"
                          fontSize="9"
                          fontFamily="Geist Mono, monospace"
                        >
                          {shortLabel}
                        </text>
                      </g>
                    );
                  })}
                </svg>
              </div>
            </section>

            {/* Status & Priority Breakdowns */}
            <section className="grid grid-cols-1 md:grid-cols-2 gap-6 font-mono text-xs">
              {/* By Status */}
              <div className="p-4 rounded bg-panel border border-line flex flex-col gap-3">
                <h2 className="text-xs uppercase tracking-wider font-bold text-content-muted pb-2 border-b border-line">
                  Breakdown by Status
                </h2>
                <div className="flex flex-col gap-2.5">
                  {STATUS_ORDER.map((statusKey) => {
                    const count = data.countsByStatus?.[statusKey] || 0;
                    const pct = data.totalBugs > 0 ? Math.round((count / data.totalBugs) * 100) : 0;
                    return (
                      <div key={statusKey} className="flex flex-col gap-1">
                        <div className="flex items-center justify-between text-[11px]">
                          <span className="text-content">{statusKey}</span>
                          <span className="text-content-muted tabular-nums">
                            {count} ({pct}%)
                          </span>
                        </div>
                        <div className="h-1.5 rounded bg-bg overflow-hidden border border-line/50">
                          <div
                            className={`h-full ${
                              statusKey === 'REOPENED'
                                ? 'bg-danger'
                                : statusKey === 'RESOLVED'
                                ? 'bg-accent'
                                : 'bg-content/60'
                            }`}
                            style={{ width: `${pct}%` }}
                          />
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* By Priority */}
              <div className="p-4 rounded bg-panel border border-line flex flex-col gap-3">
                <h2 className="text-xs uppercase tracking-wider font-bold text-content-muted pb-2 border-b border-line">
                  Breakdown by Priority
                </h2>
                <div className="flex flex-col gap-2.5">
                  {PRIORITY_ORDER.map((prioKey) => {
                    const count = data.countsByPriority?.[prioKey] || 0;
                    const pct = data.totalBugs > 0 ? Math.round((count / data.totalBugs) * 100) : 0;
                    return (
                      <div key={prioKey} className="flex flex-col gap-1">
                        <div className="flex items-center justify-between text-[11px]">
                          <span className="text-content">{prioKey}</span>
                          <span className="text-content-muted tabular-nums">
                            {count} ({pct}%)
                          </span>
                        </div>
                        <div className="h-1.5 rounded bg-bg overflow-hidden border border-line/50">
                          <div className={`h-full ${getPriorityTone(prioKey)}`} style={{ width: `${pct}%` }} />
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            </section>

            {/* Top 5 Bugs by Occurrences */}
            <section className="rounded bg-panel border border-line overflow-x-auto font-mono text-xs">
              <div className="p-4 border-b border-line flex items-center justify-between">
                <h2 className="text-xs uppercase tracking-wider font-bold text-content-muted">
                  Top 5 Bugs by Occurrences
                </h2>
              </div>
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="border-b border-line bg-panel-hover/60 text-[11px] uppercase tracking-wider text-content-muted">
                    <th scope="col" className="p-3 font-semibold w-16">ID</th>
                    <th scope="col" className="p-3 font-semibold">Title</th>
                    <th scope="col" className="p-3 font-semibold w-28">Status</th>
                    <th scope="col" className="p-3 font-semibold w-24">Priority</th>
                    <th scope="col" className="p-3 font-semibold w-28 text-right">Occurrences</th>
                    <th scope="col" className="p-3 font-semibold w-24 text-right">Sessions</th>
                    <th scope="col" className="p-3 w-10"><span className="sr-only">Open</span></th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-line/60">
                  {data.topBugsByOccurrences.map((bug) => (
                    <tr key={bug.id} className="hover:bg-panel-hover transition-colors">
                      <td className="p-3 text-content-muted font-bold tabular-nums">#{bug.id}</td>
                      <td className="p-3">
                        <Link
                          to={`/bugs/${bug.id}`}
                          className="text-content hover:text-accent transition-colors font-medium line-clamp-1"
                        >
                          {bug.title}
                        </Link>
                      </td>
                      <td className="p-3 text-[11px]">{bug.status}</td>
                      <td className="p-3 text-[11px]">{bug.priority}</td>
                      <td className="p-3 text-right font-bold text-content tabular-nums">
                        {bug.occurrenceCount}
                      </td>
                      <td className="p-3 text-right text-accent tabular-nums">
                        {bug.sessionsAffected}
                      </td>
                      <td className="p-3 text-right">
                        <Link
                          to={`/bugs/${bug.id}`}
                          aria-label={`Inspect bug #${bug.id}`}
                          className="text-content-muted hover:text-accent inline-flex"
                        >
                          <ChevronRight className="w-4 h-4" />
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </section>
          </>
        )}
      </main>
    </div>
  );
}

