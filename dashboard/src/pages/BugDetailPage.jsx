import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { api } from '../api/client';
import { useSyncState } from '../hooks/useSyncState';
import Navbar from '../components/Navbar';
import LiveActivityRail from '../components/LiveActivityRail';
import DnaStrand from '../components/DnaStrand';
import TimelinePanel from '../components/TimelinePanel';
import AlertDialog from '../components/AlertDialog';
import {
  ArrowLeft,
  Copy,
  Check,
  Trash2,
  MessageSquare,
  Sparkles,
  Clock,
  Send,
  AlertTriangle,
  User,
  ShieldAlert,
} from 'lucide-react';

export default function BugDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [bug, setBug] = useState(null);
  const [comments, setComments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isDeleted, setIsDeleted] = useState(false);

  // Form states
  const [copied, setCopied] = useState(false);
  const [patching, setPatching] = useState(false);
  const [strandPulsing, setStrandPulsing] = useState(false);
  const prevOccurrenceCountRef = useRef(null);
  const [newCommentAuthor, setNewCommentAuthor] = useState('dev');
  const [newCommentBody, setNewCommentBody] = useState('');
  const [commentSubmitting, setCommentSubmitting] = useState(false);
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
  const [deleteCommentId, setDeleteCommentId] = useState(null);

  const fetchBugData = useCallback(async () => {
    try {
      const data = await api.bugs.get(id);
      if (
        prevOccurrenceCountRef.current !== null &&
        data.occurrenceCount > prevOccurrenceCountRef.current
      ) {
        // Signature moment: pulse DNA strand once when occurrence_count rises
        setStrandPulsing(true);
        setTimeout(() => setStrandPulsing(false), 420);
      }
      prevOccurrenceCountRef.current = data.occurrenceCount;
      setBug(data);
      setIsDeleted(false);
      setError(null);
    } catch (err) {
      if (err.status === 404) {
        setIsDeleted(true);
      } else if (err.message !== 'Unauthorized') {
        setError(err.message || 'Failed to load bug detail.');
      }
    } finally {
      setLoading(false);
    }
  }, [id]);

  const fetchComments = useCallback(async () => {
    try {
      const data = await api.comments.list(id);
      setComments(data || []);
    } catch (err) {
      // If deleted, ignore
    }
  }, [id]);

  // Two-way sync hook
  const {
    syncStatus,
    secondsAgo,
    recentActivities,
    serverConflict,
    clearConflict,
    forceSync,
  } = useSyncState({
    editingBugId: Number(id),
    onBugsChanged: fetchBugData,
    onCommentChanged: fetchComments,
  });

  useEffect(() => {
    fetchBugData();
    fetchComments();
  }, [fetchBugData, fetchComments]);

  const handleCopyFingerprint = () => {
    if (bug?.fingerprint) {
      navigator.clipboard.writeText(bug.fingerprint);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  const handlePatch = async (updates) => {
    try {
      setPatching(true);
      const updated = await api.bugs.patch(id, updates);
      setBug(updated);
      clearConflict();
    } catch (err) {
      alert(`Failed to update: ${err.message}`);
    } finally {
      setPatching(false);
    }
  };

  const handleDeleteBug = async () => {
    try {
      await api.bugs.delete(id);
      navigate('/bugs');
    } catch (err) {
      alert(`Failed to delete bug: ${err.message}`);
    }
  };

  const handleAddComment = async (e) => {
    e.preventDefault();
    if (!newCommentBody.trim()) return;

    try {
      setCommentSubmitting(true);
      const created = await api.comments.create(id, {
        author: newCommentAuthor.trim() || 'dev',
        body: newCommentBody.trim(),
      });
      setComments((prev) => [...prev, created]);
      setNewCommentBody('');
    } catch (err) {
      alert(`Failed to add comment: ${err.message}`);
    } finally {
      setCommentSubmitting(false);
    }
  };

  const handleDeleteComment = async () => {
    if (!deleteCommentId) return;
    try {
      await api.comments.delete(deleteCommentId);
      setComments((prev) => prev.filter((c) => c.id !== deleteCommentId));
    } catch (err) {
      alert(`Failed to delete comment: ${err.message}`);
    } finally {
      setDeleteCommentId(null);
    }
  };

  return (
    <div className="min-h-dvh flex flex-col bg-bg text-content">
      <Navbar
        syncStatus={syncStatus}
        secondsAgo={secondsAgo}
        onForceSync={() => {
          fetchBugData();
          fetchComments();
          forceSync();
        }}
      />

      <div className="flex-1 flex flex-col lg:flex-row">
        {/* Main Content Area */}
        <main className="flex-1 p-4 md:p-6 overflow-x-hidden flex flex-col gap-6">
          {/* Breadcrumb & Navigation */}
          <div className="flex items-center justify-between gap-4 pb-3 border-b border-line">
            <Link
              to="/bugs"
              className="inline-flex items-center gap-1.5 text-xs font-mono text-content-muted hover:text-accent transition-colors"
            >
              <ArrowLeft className="w-3.5 h-3.5" />
              <span>Back to Bugs</span>
            </Link>

            {bug && (
              <button
                onClick={() => setDeleteConfirmOpen(true)}
                className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded bg-danger/10 border border-danger/30 text-danger text-xs font-mono hover:bg-danger/20 transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-danger"
              >
                <Trash2 className="w-3.5 h-3.5" />
                <span>Delete Bug</span>
              </button>
            )}
          </div>

          {/* Server Conflict Banner (Two-Way Sync Alert) */}
          {serverConflict && (
            <div className="p-3.5 rounded bg-amber-500/15 border border-warn/40 text-warn flex items-start justify-between gap-3 animate-slide-down">
              <div className="flex items-start gap-2.5 text-xs font-mono">
                <AlertTriangle className="w-4 h-4 flex-shrink-0 mt-0.5" />
                <div>
                  <span className="font-bold">Modified externally in MySQL CLI:</span>{' '}
                  <span>{serverConflict.summary}</span>
                  <div className="text-[11px] opacity-80 mt-0.5">
                    Your open view has been synchronized with the latest database state.
                  </div>
                </div>
              </div>
              <button
                onClick={clearConflict}
                className="text-xs font-mono underline hover:no-underline text-warn flex-shrink-0"
              >
                Dismiss
              </button>
            </div>
          )}

          {/* State Handling: Deleted in MySQL */}
          {isDeleted ? (
            <div className="p-12 text-center rounded border border-danger/30 bg-panel flex flex-col items-center justify-center gap-3">
              <div className="p-3 rounded-full bg-danger/10 text-danger">
                <ShieldAlert className="w-8 h-8" />
              </div>
              <h2 className="font-mono text-base font-bold text-content">
                Bug #{id} has been deleted from MySQL
              </h2>
              <p className="text-xs font-mono text-content-muted max-w-md">
                This bug was removed directly in the database or via an external API transaction.
              </p>
              <Link
                to="/bugs"
                className="mt-2 px-4 py-2 rounded bg-panel border border-line text-xs font-mono text-content hover:border-accent transition-colors"
              >
                Return to Bug List
              </Link>
            </div>
          ) : loading ? (
            /* Loading Skeleton */
            <div className="space-y-4 animate-pulse">
              <div className="h-8 bg-panel rounded w-1/2" />
              <div className="h-24 bg-panel rounded" />
              <div className="h-64 bg-panel rounded" />
            </div>
          ) : error || !bug ? (
            <div className="p-6 rounded bg-danger/10 border border-danger/30 text-danger text-xs font-mono">
              {error || 'Bug not found.'}
            </div>
          ) : (
            <>
              {/* Header: Title, Regression Tag & 64-Cell DNA Genome */}
              <div className="flex flex-col gap-4 p-4 rounded bg-panel border border-line">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                  <div className="flex flex-col gap-1">
                    <div className="flex items-center gap-2">
                      <span className="font-mono text-xs font-bold text-content-muted">
                        BUG #{bug.id}
                      </span>
                      {bug.isRegression && (
                        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-mono font-bold bg-danger/20 text-danger border border-danger/40">
                          <span className="w-1.5 h-1.5 rounded-full bg-danger animate-pulse" />
                          REGRESSION DETECTED
                        </span>
                      )}
                    </div>
                    <h1 className="font-mono text-base sm:text-lg font-bold text-content break-words">
                      {bug.title}
                    </h1>
                  </div>

                  {/* Copy Fingerprint Button */}
                  <div className="flex items-center gap-2">
                    <button
                      onClick={handleCopyFingerprint}
                      aria-label="Copy SHA-256 Fingerprint"
                      className="px-2.5 py-1.5 rounded bg-bg border border-line hover:border-accent text-xs font-mono text-content flex items-center gap-1.5 transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-accent"
                      title="Copy SHA-256 Fingerprint"
                    >
                      {copied ? (
                        <>
                          <Check className="w-3.5 h-3.5 text-accent" />
                          <span className="text-accent">Copied</span>
                        </>
                      ) : (
                        <>
                          <Copy className="w-3.5 h-3.5 text-content-muted" />
                          <span>Copy Hash</span>
                        </>
                      )}
                    </button>
                  </div>
                </div>

                {/* Fingerprint Mono Display */}
                <div className="p-2 rounded bg-bg/80 border border-line/60 flex items-center justify-between gap-2 overflow-x-auto">
                  <span className="text-[11px] font-mono text-content-muted select-all break-all">
                    {bug.fingerprint}
                  </span>
                </div>

                {/* Full 64-Cell DNA Strand Component */}
                <DnaStrand
                  fingerprint={bug.fingerprint}
                  isRegression={bug.isRegression}
                  isPulsing={strandPulsing}
                  size="lg"
                  showLabels={true}
                />
              </div>

              {/* Split Layout: Timeline on the left, Controls + AI + Comments on the right */}
              <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
                {/* Left Column (7 Cols): Click-to-Crash Timeline & Telemetry Stats */}
                <div className="lg:col-span-7 flex flex-col gap-6">
                  <TimelinePanel
                    bugId={bug.id}
                    sessionsAffected={bug.sessionsAffected}
                    occurrenceCount={bug.occurrenceCount}
                  />

                  {/* Telemetry Metrics & View Summary */}
                  <div className="p-4 rounded bg-panel border border-line flex flex-col gap-3 text-xs font-mono">
                    <h2 className="text-xs uppercase tracking-wider font-bold text-content-muted pb-2 border-b border-line">
                      Telemetry Analytics
                    </h2>

                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                      <div className="p-2.5 rounded bg-bg border border-line/60">
                        <span className="text-[10px] text-content-muted block">Occurrences</span>
                        <span className="text-base font-bold text-content tabular-nums">{bug.occurrenceCount}</span>
                      </div>

                      <div className="p-2.5 rounded bg-bg border border-line/60">
                        <span className="text-[10px] text-content-muted block">Sessions Affected</span>
                        <span className="text-base font-bold text-accent tabular-nums">{bug.sessionsAffected}</span>
                      </div>

                      <div className="p-2.5 rounded bg-bg border border-line/60">
                        <span className="text-[10px] text-content-muted block">Regression Count</span>
                        <span className={`text-base font-bold tabular-nums ${bug.regressionCount > 0 ? 'text-danger' : 'text-content'}`}>
                          {bug.regressionCount}
                        </span>
                      </div>

                      <div className="p-2.5 rounded bg-bg border border-line/60">
                        <span className="text-[10px] text-content-muted block">Minutes Open</span>
                        <span className="text-base font-bold text-content tabular-nums">
                          {bug.minutesOpen != null ? `${bug.minutesOpen}m` : '—'}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Right Column (5 Cols): Triage Controls, AI Panel & Comments */}
                <div className="lg:col-span-5 flex flex-col gap-6">
                  {/* Status, Priority & Assignee Control Panel */}
                  <div className="p-4 rounded bg-panel border border-line flex flex-col gap-4 text-xs font-mono">
                    <h2 className="text-xs uppercase tracking-wider font-bold text-content-muted pb-2 border-b border-line">
                      Status & Triage Controls
                    </h2>

                    {/* Status Select */}
                    <div className="flex flex-col gap-1.5">
                      <label htmlFor="bug-status" className="text-content-muted">
                        Status
                      </label>
                      <select
                        id="bug-status"
                        value={bug.status}
                        disabled={patching}
                        onChange={(e) => handlePatch({ status: e.target.value })}
                        className="py-1.5 px-3 rounded bg-bg border border-line text-xs font-mono text-content focus:border-accent focus:outline-none transition-colors disabled:opacity-50"
                      >
                        <option value="OPEN">OPEN</option>
                        <option value="IN_PROGRESS">IN_PROGRESS</option>
                        <option value="RESOLVED">RESOLVED</option>
                        <option value="CLOSED">CLOSED</option>
                        <option value="REOPENED">REOPENED</option>
                        <option value="IGNORED">IGNORED</option>
                      </select>
                    </div>

                    {/* Priority Select */}
                    <div className="flex flex-col gap-1.5">
                      <label htmlFor="bug-priority" className="text-content-muted">
                        Priority
                      </label>
                      <select
                        id="bug-priority"
                        value={bug.priority}
                        disabled={patching}
                        onChange={(e) => handlePatch({ priority: e.target.value })}
                        className="py-1.5 px-3 rounded bg-bg border border-line text-xs font-mono text-content focus:border-accent focus:outline-none transition-colors disabled:opacity-50"
                      >
                        <option value="LOW">LOW</option>
                        <option value="MEDIUM">MEDIUM</option>
                        <option value="HIGH">HIGH</option>
                        <option value="CRITICAL">CRITICAL</option>
                      </select>
                    </div>

                    {/* Assignee Input */}
                    <div className="flex flex-col gap-1.5">
                      <label htmlFor="bug-assignee" className="text-content-muted">
                        Assignee
                      </label>
                      <input
                        id="bug-assignee"
                        type="text"
                        defaultValue={bug.assignee || ''}
                        disabled={patching}
                        onBlur={(e) => {
                          if (e.target.value !== (bug.assignee || '')) {
                            handlePatch({ assignee: e.target.value });
                          }
                        }}
                        placeholder="Assign to team member..."
                        className="py-1.5 px-3 rounded bg-bg border border-line text-xs font-mono text-content focus:border-accent focus:outline-none transition-colors disabled:opacity-50"
                      />
                    </div>
                  </div>

                  {/* Phase 5 AI Analysis Panel Placeholder */}
                  <div className="p-4 rounded bg-panel border border-line flex flex-col gap-3 text-xs font-mono">
                    <div className="flex items-center justify-between pb-2 border-b border-line">
                      <h2 className="text-xs uppercase tracking-wider font-bold text-content-muted flex items-center gap-1.5">
                        <Sparkles className="w-3.5 h-3.5 text-accent" />
                        AI Root Cause Analysis
                      </h2>
                      <span className="text-[10px] px-1.5 py-0.5 rounded bg-line/60 text-content-muted">
                        Phase 5
                      </span>
                    </div>
                    <div className="p-4 text-center rounded bg-bg/50 border border-dashed border-line text-content-muted text-[11px] leading-relaxed">
                      Evidence-backed generative root cause analysis with citation verification will be enabled in Phase 5.
                    </div>
                  </div>

                  {/* Comments Section */}
                  <div className="p-4 rounded bg-panel border border-line flex flex-col gap-4 text-xs font-mono">
                    <div className="flex items-center justify-between pb-2 border-b border-line">
                      <h2 className="text-xs uppercase tracking-wider font-bold text-content-muted flex items-center gap-1.5">
                        <MessageSquare className="w-3.5 h-3.5" />
                        Investigation Notes ({comments.length})
                      </h2>
                    </div>

                    {/* Comments List */}
                    <div className="flex flex-col gap-2.5">
                      {comments.length === 0 ? (
                        <p className="text-content-muted py-2 text-center text-[11px]">
                          No investigation notes yet. Add one below or from the MySQL CLI!
                        </p>
                      ) : (
                        comments.map((c) => (
                          <div
                            key={c.id}
                            className="p-3 rounded bg-bg border border-line/60 flex flex-col gap-1.5"
                          >
                            <div className="flex items-center justify-between">
                              <span className="font-semibold text-accent flex items-center gap-1">
                                <User className="w-3 h-3 text-content-muted" />
                                {c.author}
                              </span>
                              <div className="flex items-center gap-2">
                                <span className="text-[10px] text-content-muted">
                                  {new Date(c.createdAt).toLocaleTimeString([], {
                                    hour: '2-digit',
                                    minute: '2-digit',
                                  })}
                                </span>
                                <button
                                  onClick={() => setDeleteCommentId(c.id)}
                                  aria-label="Delete comment"
                                  className="text-content-muted hover:text-danger p-0.5 transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-danger rounded"
                                  title="Delete comment"
                                >
                                  <Trash2 className="w-3 h-3" />
                                </button>
                              </div>
                            </div>
                            <p className="text-content whitespace-pre-wrap leading-relaxed">
                              {c.body}
                            </p>
                          </div>
                        ))
                      )}
                    </div>

                    {/* Add Comment Form */}
                    <form onSubmit={handleAddComment} className="flex flex-col gap-2 pt-2 border-t border-line">
                      <div className="flex items-center gap-2">
                        <input
                          type="text"
                          value={newCommentAuthor}
                          onChange={(e) => setNewCommentAuthor(e.target.value)}
                          placeholder="Author"
                          className="w-32 py-1 px-2.5 rounded bg-bg border border-line text-xs font-mono text-content focus:border-accent focus:outline-none"
                        />
                        <span className="text-[10px] text-content-muted">adding as author</span>
                      </div>
                      <div className="flex gap-2">
                        <textarea
                          rows={2}
                          value={newCommentBody}
                          onChange={(e) => setNewCommentBody(e.target.value)}
                          placeholder="Add engineering investigation note..."
                          className="flex-1 p-2 rounded bg-bg border border-line text-xs font-mono text-content placeholder:text-content-muted/50 focus:border-accent focus:outline-none resize-none"
                        />
                        <button
                          type="submit"
                          disabled={commentSubmitting || !newCommentBody.trim()}
                          className="px-3 py-2 rounded bg-accent text-bg font-mono font-semibold flex items-center justify-center hover:bg-accent/90 transition-colors disabled:opacity-40"
                        >
                          <Send className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </form>
                  </div>
                </div>
              </div>
            </>
          )}
        </main>

        {/* Live Activity Feed Rail */}
        <LiveActivityRail activities={recentActivities} />
      </div>

      {/* Delete Confirmation Dialog */}
      <AlertDialog
        isOpen={deleteConfirmOpen}
        title={`Delete Bug #${id}?`}
        description="This will permanently delete this bug and its associated comments and occurrence records from MySQL. Telemetry events will be preserved."
        confirmText="Delete Permanently"
        onConfirm={handleDeleteBug}
        onCancel={() => setDeleteConfirmOpen(false)}
      />

      {/* Delete Comment Dialog */}
      <AlertDialog
        isOpen={Boolean(deleteCommentId)}
        title="Delete Comment?"
        description="This will remove the comment from the database and log a DELETE event to the live activity feed."
        confirmText="Delete"
        onConfirm={handleDeleteComment}
        onCancel={() => setDeleteCommentId(null)}
      />
    </div>
  );
}

