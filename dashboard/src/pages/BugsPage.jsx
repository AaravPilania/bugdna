import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import { api } from '../api/client';
import { useSyncState } from '../hooks/useSyncState';
import Navbar from '../components/Navbar';
import LiveActivityRail from '../components/LiveActivityRail';
import DnaStrand from '../components/DnaStrand';
import { Search, Filter, ArrowUpDown, ChevronRight, AlertCircle, RefreshCw } from 'lucide-react';

function getStatusBadge(status) {
  const styles = {
    OPEN: 'bg-sky-500/15 text-sky-400 border-sky-500/30',
    IN_PROGRESS: 'bg-amber-500/15 text-warn border-warn/30',
    RESOLVED: 'bg-emerald-500/15 text-accent border-accent/30',
    CLOSED: 'bg-zinc-700/20 text-content-muted border-zinc-700/50',
    REOPENED: 'bg-rose-500/20 text-danger border-danger/40',
    IGNORED: 'bg-zinc-800 text-zinc-500 border-zinc-700',
  };
  return (
    <span
      className={`px-2 py-0.5 rounded text-[10px] font-mono font-semibold uppercase tracking-wider border ${
        styles[status] || styles.OPEN
      }`}
    >
      {status}
    </span>
  );
}

function getPriorityBadge(priority) {
  const styles = {
    LOW: 'text-content-muted',
    MEDIUM: 'text-sky-300',
    HIGH: 'text-warn font-medium',
    CRITICAL: 'text-danger font-bold',
  };
  return (
    <span className={`text-[11px] font-mono uppercase tracking-wider ${styles[priority] || ''}`}>
      {priority}
    </span>
  );
}

function formatRelativeTime(dateString) {
  if (!dateString) return '—';
  const now = Date.now();
  const date = new Date(dateString).getTime();
  const diffSec = Math.max(0, Math.floor((now - date) / 1000));

  if (diffSec < 10) return 'just now';
  if (diffSec < 60) return `${diffSec}s ago`;
  const diffMin = Math.floor(diffSec / 60);
  if (diffMin < 60) return `${diffMin}m ago`;
  const diffHour = Math.floor(diffMin / 60);
  if (diffHour < 24) return `${diffHour}h ago`;
  return `${Math.floor(diffHour / 24)}d ago`;
}

export default function BugsPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  // Read URL filter params
  const statusParam = searchParams.get('status') || '';
  const priorityParam = searchParams.get('priority') || '';
  const errorTypeParam = searchParams.get('errorType') || '';
  const queryParam = searchParams.get('q') || '';
  const sortParam = searchParams.get('sort') || 'lastSeen';

  const [bugs, setBugs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchInput, setSearchInput] = useState(queryParam);
  const [pulsingBugIds, setPulsingBugIds] = useState(new Set());
  const prevCountsRef = useRef(new Map());

  const fetchBugs = useCallback(async () => {
    try {
      setError(null);
      const data = await api.bugs.list({
        status: statusParam || undefined,
        priority: priorityParam || undefined,
        errorType: errorTypeParam || undefined,
        q: queryParam || undefined,
        sort: sortParam || undefined,
      });
      const list = data || [];
      const newlyPulsed = [];
      if (prevCountsRef.current.size > 0) {
        list.forEach((b) => {
          const oldCount = prevCountsRef.current.get(b.id);
          if (oldCount === undefined || b.occurrenceCount > oldCount) {
            newlyPulsed.push(b.id);
          }
        });
      }
      const nextMap = new Map();
      list.forEach((b) => nextMap.set(b.id, b.occurrenceCount));
      prevCountsRef.current = nextMap;

      if (newlyPulsed.length > 0) {
        setPulsingBugIds((prev) => new Set([...prev, ...newlyPulsed]));
        setTimeout(() => {
          setPulsingBugIds((prev) => {
            const cleaned = new Set(prev);
            newlyPulsed.forEach((id) => cleaned.delete(id));
            return cleaned;
          });
        }, 420);
      }

      setBugs(list);
    } catch (err) {
      if (err.message !== 'Unauthorized') {
        setError(err.message || 'Failed to load bugs.');
      }
    } finally {
      setLoading(false);
    }
  }, [statusParam, priorityParam, errorTypeParam, queryParam, sortParam]);

  // Two-way sync hook
  const {
    syncStatus,
    secondsAgo,
    recentActivities,
    changedBugIds,
    forceSync,
  } = useSyncState({
    onBugsChanged: fetchBugs,
  });

  useEffect(() => {
    fetchBugs();
  }, [fetchBugs]);

  // Sync debounced search
  useEffect(() => {
    const timer = setTimeout(() => {
      if (searchInput !== queryParam) {
        const next = new URLSearchParams(searchParams);
        if (searchInput.trim()) {
          next.set('q', searchInput.trim());
        } else {
          next.delete('q');
        }
        setSearchParams(next);
      }
    }, 300);
    return () => clearTimeout(timer);
  }, [searchInput, queryParam, searchParams, setSearchParams]);

  const updateFilter = (key, value) => {
    const next = new URLSearchParams(searchParams);
    if (value) {
      next.set(key, value);
    } else {
      next.delete(key);
    }
    setSearchParams(next);
  };

  return (
    <div className="min-h-dvh flex flex-col bg-bg text-content">
      <Navbar
        syncStatus={syncStatus}
        secondsAgo={secondsAgo}
        onForceSync={() => {
          fetchBugs();
          forceSync();
        }}
      />

      <div className="flex-1 flex flex-col lg:flex-row">
        {/* Main Content Area */}
        <main className="flex-1 p-4 md:p-6 overflow-x-hidden flex flex-col gap-4">
          {/* Header & Metric bar */}
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-2 border-b border-line">
            <div>
              <h1 className="text-lg font-mono font-bold tracking-tight text-content flex items-center gap-2">
                <span>Recorded Bugs</span>
                <span className="px-2 py-0.5 rounded text-xs bg-panel border border-line text-accent font-mono font-medium">
                  {bugs.length}
                </span>
              </h1>
              <p className="text-xs font-mono text-content-muted mt-0.5">
                Deterministic SHA-256 genomes mapped to live browser telemetry
              </p>
            </div>
          </div>

          {/* Filters Toolbar */}
          <div className="flex flex-wrap items-center gap-2.5 p-3 rounded bg-panel border border-line text-xs font-mono">
            {/* Search Input */}
            <div className="relative flex-1 min-w-[200px]">
              <Search className="w-3.5 h-3.5 text-content-muted absolute left-2.5 top-2.5 pointer-events-none" />
              <input
                type="text"
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                placeholder="Search title, fingerprint or endpoint..."
                className="w-full pl-8 pr-3 py-1.5 rounded bg-bg border border-line text-xs text-content placeholder:text-content-muted/50 focus:border-accent focus:ring-1 focus:ring-accent focus:outline-none transition-colors"
                aria-label="Search bugs"
              />
            </div>

            {/* Status Filter */}
            <select
              value={statusParam}
              onChange={(e) => updateFilter('status', e.target.value)}
              className="py-1.5 px-2.5 rounded bg-bg border border-line text-xs text-content focus:border-accent focus:outline-none transition-colors"
              aria-label="Filter by status"
            >
              <option value="">Status: All</option>
              <option value="OPEN">OPEN</option>
              <option value="IN_PROGRESS">IN_PROGRESS</option>
              <option value="RESOLVED">RESOLVED</option>
              <option value="CLOSED">CLOSED</option>
              <option value="REOPENED">REOPENED</option>
              <option value="IGNORED">IGNORED</option>
            </select>

            {/* Priority Filter */}
            <select
              value={priorityParam}
              onChange={(e) => updateFilter('priority', e.target.value)}
              className="py-1.5 px-2.5 rounded bg-bg border border-line text-xs text-content focus:border-accent focus:outline-none transition-colors"
              aria-label="Filter by priority"
            >
              <option value="">Priority: All</option>
              <option value="CRITICAL">CRITICAL</option>
              <option value="HIGH">HIGH</option>
              <option value="MEDIUM">MEDIUM</option>
              <option value="LOW">LOW</option>
            </select>

            {/* Error Type Filter */}
            <select
              value={errorTypeParam}
              onChange={(e) => updateFilter('errorType', e.target.value)}
              className="py-1.5 px-2.5 rounded bg-bg border border-line text-xs text-content focus:border-accent focus:outline-none transition-colors"
              aria-label="Filter by error type"
            >
              <option value="">Type: All</option>
              <option value="JS_ERROR">JS_ERROR</option>
              <option value="API_ERROR">API_ERROR</option>
              <option value="PROMISE_REJECTION">PROMISE_REJECTION</option>
              <option value="NETWORK_ERROR">NETWORK_ERROR</option>
            </select>

            {/* Sort Order */}
            <select
              value={sortParam}
              onChange={(e) => updateFilter('sort', e.target.value)}
              className="py-1.5 px-2.5 rounded bg-bg border border-line text-xs text-content focus:border-accent focus:outline-none transition-colors"
              aria-label="Sort bugs"
            >
              <option value="lastSeen">Sort: Last Seen</option>
              <option value="occurrences">Sort: Occurrences</option>
              <option value="priority">Sort: Priority</option>
            </select>
          </div>

          {/* Table Container */}
          {error ? (
            <div className="p-6 rounded bg-danger/10 border border-danger/30 text-danger text-xs font-mono flex items-center gap-2">
              <AlertCircle className="w-4 h-4 flex-shrink-0" />
              <span>{error}</span>
            </div>
          ) : loading ? (
            /* Loading Skeleton */
            <div className="rounded border border-line bg-panel divide-y divide-line overflow-hidden">
              {[...Array(6)].map((_, i) => (
                <div key={i} className="p-3.5 flex items-center justify-between gap-4 animate-pulse">
                  <div className="flex-1 space-y-2">
                    <div className="h-4 bg-line/60 rounded w-2/3" />
                    <div className="h-2.5 bg-line/40 rounded w-1/3" />
                  </div>
                  <div className="w-24 h-6 bg-line/40 rounded" />
                </div>
              ))}
            </div>
          ) : bugs.length === 0 ? (
            /* Empty State */
            <div className="p-12 text-center rounded border border-line bg-panel flex flex-col items-center justify-center gap-3">
              <div className="w-10 h-10 rounded-full bg-bg border border-line flex items-center justify-center text-xl">
                🔍
              </div>
              <h3 className="font-mono text-sm font-semibold text-content">No bugs found</h3>
              <p className="text-xs font-mono text-content-muted max-w-sm">
                No telemetry matches the current filters. Adjust your query or trigger actions in the demo store.
              </p>
            </div>
          ) : (
            /* Dense Bug Table */
            <div className="rounded border border-line bg-panel overflow-x-auto shadow-sm">
              <table className="w-full text-left border-collapse text-xs font-mono">
                <thead>
                  <tr className="border-b border-line bg-panel-hover/60 text-[11px] uppercase tracking-wider text-content-muted">
                    <th scope="col" className="p-3 font-semibold w-16">ID</th>
                    <th scope="col" className="p-3 font-semibold">Bug Title & DNA Genome</th>
                    <th scope="col" className="p-3 font-semibold w-28">Status</th>
                    <th scope="col" className="p-3 font-semibold w-24">Priority</th>
                    <th scope="col" className="p-3 font-semibold w-28">Assignee</th>
                    <th scope="col" className="p-3 font-semibold w-24 text-right">Occurrences</th>
                    <th scope="col" className="p-3 font-semibold w-28 text-right">Last Seen</th>
                    <th scope="col" className="p-3 font-semibold w-12"><span className="sr-only">Actions</span></th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-line/60">
                  {bugs.map((bug) => {
                    const isChanged = changedBugIds.has(bug.id);
                    return (
                      <tr
                        key={bug.id}
                        onClick={() => navigate(`/bugs/${bug.id}`)}
                        className={`cursor-pointer transition-colors duration-150 hover:bg-panel-hover ${
                          isChanged ? 'animate-highlight-flash bg-accent/10' : ''
                        }`}
                      >
                        {/* ID */}
                        <td className="p-3 text-content-muted font-bold whitespace-nowrap">
                          #{bug.id}
                        </td>

                        {/* Title & DNA Strand */}
                        <td className="p-3 min-w-[280px]">
                          <div className="flex flex-col gap-1.5">
                            <div className="flex items-center gap-2">
                              <span className="font-medium text-content hover:text-accent transition-colors line-clamp-1">
                                {bug.title}
                              </span>
                              {bug.isRegression && (
                                <span className="inline-flex items-center px-1.5 py-0.2 rounded text-[9px] font-bold bg-danger/20 text-danger border border-danger/40 tracking-wider">
                                  REGRESSION
                                </span>
                              )}
                            </div>
                            {/* DNA Strip */}
                            <div className="w-full max-w-md">
                              <DnaStrand
                                fingerprint={bug.fingerprint}
                                isRegression={bug.isRegression}
                                isPulsing={pulsingBugIds.has(bug.id) || isChanged}
                                size="sm"
                              />
                            </div>
                          </div>
                        </td>

                        {/* Status */}
                        <td className="p-3 whitespace-nowrap">
                          {getStatusBadge(bug.status)}
                        </td>

                        {/* Priority */}
                        <td className="p-3 whitespace-nowrap">
                          {getPriorityBadge(bug.priority)}
                        </td>

                        {/* Assignee */}
                        <td className="p-3 text-content-muted whitespace-nowrap">
                          {bug.assignee || 'Unassigned'}
                        </td>

                        {/* Occurrences */}
                        <td className="p-3 text-right font-semibold text-content whitespace-nowrap tabular-nums">
                          {bug.occurrenceCount}
                        </td>

                        {/* Last Seen */}
                        <td className="p-3 text-right text-content-muted whitespace-nowrap tabular-nums">
                          {formatRelativeTime(bug.lastSeenAt)}
                        </td>

                        {/* Chevron link */}
                        <td className="p-3 text-right text-content-muted">
                          <ChevronRight className="w-4 h-4 ml-auto" />
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </main>

        {/* Live Activity Feed Rail */}
        <LiveActivityRail activities={recentActivities} />
      </div>
    </div>
  );
}

