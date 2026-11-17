import { useState, useEffect, useRef, useCallback } from 'react';
import { api } from '../api/client';

export function useSyncState({
  onBugsChanged,
  onActivityAdded,
  onCommentChanged,
  editingBugId = null,
} = {}) {
  const [syncStatus, setSyncStatus] = useState('synced'); // 'synced', 'syncing', 'error'
  const [lastSyncTime, setLastSyncTime] = useState(Date.now());
  const [secondsAgo, setSecondsAgo] = useState(0);
  const [recentActivities, setRecentActivities] = useState([]);
  const [changedBugIds, setChangedBugIds] = useState(new Set());
  const [serverConflict, setServerConflict] = useState(null);

  const lastStateRef = useRef({
    maxActivityId: 0,
    bugsMaxUpdatedAt: null,
    bugCount: -1,
    commentCount: -1,
  });

  const abortControllerRef = useRef(null);
  const timerRef = useRef(null);
  const isMountedRef = useRef(true);

  // Clear highlight on bug rows after 1.2s
  const triggerRowHighlight = useCallback((ids) => {
    if (!ids || ids.length === 0) return;
    setChangedBugIds((prev) => new Set([...prev, ...ids]));
    setTimeout(() => {
      if (isMountedRef.current) {
        setChangedBugIds((prev) => {
          const next = new Set(prev);
          ids.forEach((id) => next.delete(id));
          return next;
        });
      }
    }, 1200);
  }, []);

  const pollSync = useCallback(async () => {
    if (!isMountedRef.current || document.hidden) return;

    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }
    abortControllerRef.current = new AbortController();

    try {
      setSyncStatus('syncing');
      const state = await api.sync.getState(abortControllerRef.current.signal);
      if (!isMountedRef.current) return;

      const prev = lastStateRef.current;
      const activityChanged = prev.maxActivityId > 0 && state.maxActivityId > prev.maxActivityId;
      const bugsChanged =
        prev.bugsMaxUpdatedAt !== null &&
        (state.bugsMaxUpdatedAt !== prev.bugsMaxUpdatedAt || state.bugCount !== prev.bugCount);
      const commentsChanged =
        prev.commentCount !== -1 && state.commentCount !== prev.commentCount;

      // 1. Fetch new activities if any new events were logged
      if (activityChanged || prev.maxActivityId === 0) {
        const afterId = prev.maxActivityId > 0 ? prev.maxActivityId : null;
        const newFeed = await api.activity.get(afterId, 25);
        if (isMountedRef.current && newFeed) {
          setRecentActivities((old) => {
            const existingIds = new Set(old.map((a) => a.id));
            const uniqueIncoming = newFeed.filter((a) => !existingIds.has(a.id));
            return [...uniqueIncoming, ...old].slice(0, 50);
          });

          // Check if any activity affects bugs
          const affectedBugs = newFeed
            .filter((a) => a.entityType === 'BUG')
            .map((a) => a.bugId)
            .filter(Boolean);

          if (affectedBugs.length > 0) {
            triggerRowHighlight(affectedBugs);
          }

          // Check for conflicts if the user is currently editing a bug
          if (editingBugId) {
            const conflictAct = newFeed.find(
              (a) => a.entityType === 'BUG' && a.bugId === editingBugId && a.source === 'MYSQL_CLI'
            );
            if (conflictAct) {
              setServerConflict({
                summary: conflictAct.summary,
                source: conflictAct.source,
                timestamp: conflictAct.createdAt,
              });
            }
          }

          if (onActivityAdded && newFeed.length > 0) {
            onActivityAdded(newFeed);
          }
        }
      }

      // 2. Refresh bugs list/details if bugs or comments changed
      if (bugsChanged && onBugsChanged) {
        onBugsChanged();
      }

      if (commentsChanged && onCommentChanged) {
        onCommentChanged();
      }

      lastStateRef.current = {
        maxActivityId: state.maxActivityId,
        bugsMaxUpdatedAt: state.bugsMaxUpdatedAt,
        bugCount: state.bugCount,
        commentCount: state.commentCount,
      };

      setLastSyncTime(Date.now());
      setSyncStatus('synced');
    } catch (err) {
      if (err.name !== 'AbortError' && isMountedRef.current) {
        setSyncStatus('error');
      }
    } finally {
      if (isMountedRef.current && !document.hidden) {
        timerRef.current = setTimeout(pollSync, 5000);
      }
    }
  }, [onBugsChanged, onActivityAdded, onCommentChanged, editingBugId, triggerRowHighlight]);

  // Handle visibility change: pause when hidden, refresh immediately on focus
  useEffect(() => {
    isMountedRef.current = true;

    const handleVisibility = () => {
      if (document.hidden) {
        if (timerRef.current) clearTimeout(timerRef.current);
      } else {
        pollSync();
      }
    };

    document.addEventListener('visibilitychange', handleVisibility);
    pollSync();

    return () => {
      isMountedRef.current = false;
      document.removeEventListener('visibilitychange', handleVisibility);
      if (timerRef.current) clearTimeout(timerRef.current);
      if (abortControllerRef.current) abortControllerRef.current.abort();
    };
  }, [pollSync]);

  // Update "Synced Ns ago" ticker every second
  useEffect(() => {
    const ticker = setInterval(() => {
      const elapsed = Math.floor((Date.now() - lastSyncTime) / 1000);
      setSecondsAgo(elapsed);
    }, 1000);
    return () => clearInterval(ticker);
  }, [lastSyncTime]);

  return {
    syncStatus,
    secondsAgo,
    recentActivities,
    changedBugIds,
    serverConflict,
    clearConflict: () => setServerConflict(null),
    forceSync: pollSync,
  };
}

