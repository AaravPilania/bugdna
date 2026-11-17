import React, { useEffect, useState, useCallback } from 'react';
import { api } from '../api/client';
import {
  Clock,
  MousePointerClick,
  Compass,
  Eye,
  AlertOctagon,
  WifiOff,
  Flame,
  Users,
} from 'lucide-react';

function formatOffset(offsetMs) {
  if (offsetMs === 0) return '0.0 s';
  const sign = offsetMs < 0 ? '−' : '+';
  const secs = (Math.abs(offsetMs) / 1000).toFixed(1);
  return `${sign}${secs} s`;
}

function formatClock(dateString) {
  if (!dateString) return '—';
  const d = new Date(dateString);
  return d.toLocaleTimeString([], {
    hour12: false,
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
}

function getEventVisual(type, isTrigger) {
  if (isTrigger) {
    return {
      icon: <Flame className="w-3.5 h-3.5 text-danger" />,
      badge: 'bg-danger/20 text-danger border-danger/40',
      dot: 'bg-danger ring-4 ring-danger/20',
    };
  }
  switch (type) {
    case 'CLICK':
      return {
        icon: <MousePointerClick className="w-3.5 h-3.5 text-accent" />,
        badge: 'bg-accent/10 text-accent border-accent/30',
        dot: 'bg-accent',
      };
    case 'ROUTE_CHANGE':
      return {
        icon: <Compass className="w-3.5 h-3.5 text-sky-400" />,
        badge: 'bg-sky-500/10 text-sky-400 border-sky-500/30',
        dot: 'bg-sky-400',
      };
    case 'PAGE_VIEW':
      return {
        icon: <Eye className="w-3.5 h-3.5 text-content-muted" />,
        badge: 'bg-line/60 text-content border-line',
        dot: 'bg-content-muted',
      };
    case 'NETWORK_ERROR':
      return {
        icon: <WifiOff className="w-3.5 h-3.5 text-warn" />,
        badge: 'bg-warn/15 text-warn border-warn/30',
        dot: 'bg-warn',
      };
    case 'API_ERROR':
    case 'JS_ERROR':
    case 'PROMISE_REJECTION':
      return {
        icon: <AlertOctagon className="w-3.5 h-3.5 text-danger" />,
        badge: 'bg-danger/15 text-danger border-danger/30',
        dot: 'bg-danger',
      };
    default:
      return {
        icon: <Clock className="w-3.5 h-3.5 text-content-muted" />,
        badge: 'bg-line/60 text-content-muted border-line',
        dot: 'bg-content-muted',
      };
  }
}

export default function TimelinePanel({
  bugId,
  sessionsAffected = 0,
  occurrenceCount = 0,
  highlightedEventId = null,
}) {
  const [occurrences, setOccurrences] = useState([]);
  const [selectedOccId, setSelectedOccId] = useState('');
  const [timeline, setTimeline] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const loadOccurrencesAndTimeline = useCallback(
    async (occIdToLoad = null) => {
      try {
        setError(null);
        const occList = await api.bugs.occurrences(bugId);
        setOccurrences(occList || []);

        const targetOccId = occIdToLoad || (occList && occList.length > 0 ? occList[0].id : null);
        if (targetOccId) {
          setSelectedOccId(String(targetOccId));
        }

        const tl = await api.bugs.timeline(bugId, targetOccId || undefined);
        setTimeline(tl);
      } catch (err) {
        setError(err.message || 'Could not load session timeline.');
      } finally {
        setLoading(false);
      }
    },
    [bugId]
  );

  // Refetch timeline whenever bugId or occurrenceCount changes (e.g. live poll saw new occurrence)
  useEffect(() => {
    loadOccurrencesAndTimeline();
  }, [loadOccurrencesAndTimeline, occurrenceCount]);

  // Scroll to highlighted evidence event (used by Phase 5 AI panel chips)
  useEffect(() => {
    if (!highlightedEventId) return;
    const el = document.getElementById(`timeline-event-${highlightedEventId}`);
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
  }, [highlightedEventId]);

  const handleSelectOccurrence = async (e) => {
    const newOccId = e.target.value;
    setSelectedOccId(newOccId);
    setLoading(true);
    try {
      const tl = await api.bugs.timeline(bugId, newOccId);
      setTimeline(tl);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="p-4 rounded bg-panel border border-line flex flex-col gap-4 text-xs font-mono">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-3 border-b border-line">
        <div className="flex items-center gap-2">
          <Clock className="w-4 h-4 text-accent" />
          <h2 className="text-xs uppercase tracking-wider font-bold text-content">
            Click-to-Crash Timeline
          </h2>
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded bg-bg border border-line text-[11px] text-content-muted tabular-nums">
            <Users className="w-3 h-3 text-accent" />
            {sessionsAffected} session{sessionsAffected === 1 ? '' : 's'}
          </span>
        </div>

        {/* Occurrence Switcher */}
        {occurrences.length > 0 && (
          <select
            aria-label="Select bug occurrence"
            value={selectedOccId}
            onChange={handleSelectOccurrence}
            className="py-1 px-2 rounded bg-bg border border-line text-[11px] font-mono text-content focus:border-accent focus:outline-none max-w-[260px]"
          >
            {occurrences.map((occ, idx) => (
              <option key={occ.id} value={occ.id}>
                #{occ.id} {idx === 0 ? '(Latest) ' : ''}· {formatClock(occ.occurredAt)} ·{' '}
                {(occ.sessionId || '').slice(0, 8)}
                {occ.wasRegression ? ' [REGRESSION]' : ''}
              </option>
            ))}
          </select>
        )}
      </div>

      {/* Session Meta */}
      {timeline?.sessionId && (
        <div className="flex items-center justify-between text-[11px] text-content-muted bg-bg/60 px-2.5 py-1.5 rounded border border-line/60">
          <span>
            Session: <span className="text-content">{timeline.sessionId}</span>
          </span>
          <span className="tabular-nums">Window: −120 s … +10 s</span>
        </div>
      )}

      {/* Body */}
      {error ? (
        <div className="p-3 rounded bg-danger/10 border border-danger/30 text-danger text-xs">
          {error}
        </div>
      ) : loading ? (
        <div className="space-y-3 animate-pulse py-2">
          {[1, 2, 3, 4].map((n) => (
            <div key={n} className="flex items-center gap-3">
              <div className="w-12 h-4 bg-line/60 rounded" />
              <div className="flex-1 h-10 bg-line/40 rounded" />
            </div>
          ))}
        </div>
      ) : !timeline || !timeline.events || timeline.events.length === 0 ? (
        <div className="p-6 text-center rounded bg-bg/50 border border-dashed border-line text-content-muted text-[11px]">
          No recorded session events found in the −120 s … +10 s window for this bug.
        </div>
      ) : (
        <ol className="relative flex flex-col gap-2.5 pl-4 before:content-[''] before:absolute before:left-[6px] before:top-2 before:bottom-2 before:w-px before:bg-line">
          {timeline.events.map((ev) => {
            const visual = getEventVisual(ev.type, ev.isTrigger);
            const isHighlighted =
              highlightedEventId &&
              (String(highlightedEventId) === String(ev.eventId) ||
                String(highlightedEventId) === String(ev.id));

            return (
              <li
                key={ev.id}
                id={`timeline-event-${ev.eventId}`}
                className={`relative pl-4 transition-all ${
                  isHighlighted ? 'ring-1 ring-accent rounded' : ''
                }`}
              >
                {/* Vertical rail node */}
                <span
                  className={`absolute -left-[14px] top-3 w-2.5 h-2.5 rounded-full ${visual.dot}`}
                />

                <div
                  className={`p-2.5 rounded border flex flex-col gap-1.5 ${
                    ev.isTrigger
                      ? 'bg-danger/10 border-danger/50'
                      : 'bg-bg/70 border-line/70 hover:border-line'
                  }`}
                >
                  {/* Top row: offset, type pill, action/endpoint, wall clock */}
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span
                        className={`text-[11px] font-bold tabular-nums min-w-[54px] ${
                          ev.isTrigger ? 'text-danger' : 'text-content-muted'
                        }`}
                      >
                        {formatOffset(ev.offsetMs)}
                      </span>

                      <span
                        className={`inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-semibold border ${visual.badge}`}
                      >
                        {visual.icon}
                        {ev.type}
                      </span>

                      {ev.actionLabel && (
                        <span className="px-1.5 py-0.5 rounded bg-panel border border-line text-[10px] font-bold text-accent">
                          {ev.actionLabel}
                        </span>
                      )}

                      {ev.httpMethod && ev.httpStatus && (
                        <span className="text-[11px] font-semibold text-content tabular-nums">
                          {ev.httpMethod} {ev.httpStatus}
                        </span>
                      )}

                      {ev.errorCode && (
                        <span className="px-1.5 py-0.5 rounded bg-danger/15 text-danger text-[10px] font-bold">
                          {ev.errorCode}
                        </span>
                      )}

                      {ev.isTrigger && (
                        <span className="px-1.5 py-0.5 rounded bg-danger text-white text-[9px] font-bold uppercase tracking-wider">
                          TRIGGER
                        </span>
                      )}
                    </div>

                    <span className="text-[10px] text-content-muted tabular-nums">
                      {formatClock(ev.occurredAt)}
                    </span>
                  </div>

                  {/* Secondary details: page path / endpoint / sanitized message */}
                  <div className="flex flex-col gap-0.5 text-[11px] text-content-muted pl-[62px]">
                    {(ev.endpoint || ev.pagePath) && (
                      <div className="truncate">
                        {ev.endpoint ? (
                          <span className="text-content/90">{ev.endpoint}</span>
                        ) : (
                          <span>path: {ev.pagePath}</span>
                        )}
                        {ev.endpoint && ev.pagePath && (
                          <span className="ml-2 opacity-70">on {ev.pagePath}</span>
                        )}
                      </div>
                    )}
                    {ev.message && (
                      <p className="text-content/90 break-words leading-snug">{ev.message}</p>
                    )}
                    {ev.stackTop && (
                      <code className="text-[10px] text-content-muted bg-panel/80 px-1.5 py-0.5 rounded border border-line/50 block truncate">
                        {ev.stackTop}
                      </code>
                    )}
                  </div>
                </div>
              </li>
            );
          })}
        </ol>
      )}
    </section>
  );
}
