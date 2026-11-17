import React, { useEffect, useState, useCallback } from 'react';
import { api } from '../api/client';
import {
  Sparkles,
  Loader2,
  RefreshCw,
  CheckCircle2,
  HelpCircle,
  Wrench,
  ListOrdered,
  Copy,
  Check,
  AlertTriangle,
  Link2,
} from 'lucide-react';

function getConfidenceStyle(confidence) {
  switch ((confidence || '').toLowerCase()) {
    case 'high':
      return 'bg-danger/15 text-danger border-danger/40';
    case 'low':
      return 'bg-line/60 text-content-muted border-line';
    case 'medium':
    default:
      return 'bg-warn/15 text-warn border-warn/40';
  }
}

export default function AiAnalysisPanel({ bugId, bugTitle = '', onSelectEvidenceEvent }) {
  const [analysis, setAnalysis] = useState(null);
  const [initialLoading, setInitialLoading] = useState(true);
  const [running, setRunning] = useState(false);
  const [error, setError] = useState(null);
  const [copiedRepro, setCopiedRepro] = useState(false);

  const loadLatest = useCallback(async () => {
    try {
      setError(null);
      const data = await api.bugs.getAnalysis(bugId);
      setAnalysis(data);
    } catch (err) {
      setError(err.message || 'Could not check previous AI analysis.');
    } finally {
      setInitialLoading(false);
    }
  }, [bugId]);

  useEffect(() => {
    loadLatest();
  }, [loadLatest]);

  const handleAnalyze = async (force = false) => {
    try {
      setRunning(true);
      setError(null);
      const data = await api.bugs.runAnalysis(bugId, force);
      setAnalysis(data);
    } catch (err) {
      setError(err.message || 'Failed to run AI analysis.');
    } finally {
      setRunning(false);
    }
  };

  const handleCopyReproMarkdown = () => {
    if (!analysis?.reproductionSteps?.length) return;
    const md = [
      `### Reproduction Steps — Bug #${bugId}${bugTitle ? ` (${bugTitle})` : ''}`,
      ...analysis.reproductionSteps.map((step, idx) => `${idx + 1}. ${step}`),
    ].join('\n');

    navigator.clipboard.writeText(md);
    setCopiedRepro(true);
    setTimeout(() => setCopiedRepro(false), 2000);
  };

  return (
    <section className="p-4 rounded bg-panel border border-line flex flex-col gap-4 text-xs font-mono">
      {/* Header */}
      <div className="flex flex-wrap items-center justify-between gap-2 pb-3 border-b border-line">
        <div className="flex items-center gap-2">
          <Sparkles className="w-4 h-4 text-accent" />
          <h2 className="text-xs uppercase tracking-wider font-bold text-content">
            AI Root Cause Analysis
          </h2>
          {analysis?.status === 'MOCK' && (
            <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-warn/15 text-warn border border-warn/40">
              MOCK
            </span>
          )}
          {analysis?.cached && (
            <span className="px-1.5 py-0.5 rounded text-[10px] bg-line/60 text-content-muted border border-line">
              CACHED
            </span>
          )}
        </div>

        <div className="flex items-center gap-2">
          {analysis && (
            <button
              type="button"
              disabled={running}
              onClick={() => handleAnalyze(true)}
              aria-label="Re-run AI analysis bypassing cache"
              className="px-2 py-1 rounded bg-bg border border-line text-[11px] text-content-muted hover:text-content hover:border-accent flex items-center gap-1 transition-colors disabled:opacity-50"
            >
              <RefreshCw className={`w-3 h-3 ${running ? 'animate-spin' : ''}`} />
              <span>Re-run</span>
            </button>
          )}

          <button
            type="button"
            disabled={running}
            onClick={() => handleAnalyze(false)}
            className="px-3 py-1.5 rounded bg-accent text-bg font-semibold text-xs flex items-center gap-1.5 hover:bg-accent/90 transition-colors disabled:opacity-60 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent"
          >
            {running ? (
              <Loader2 className="w-3.5 h-3.5 animate-spin" />
            ) : (
              <Sparkles className="w-3.5 h-3.5" />
            )}
            <span>Analyze with AI</span>
          </button>
        </div>
      </div>

      {error && (
        <div className="p-3 rounded bg-danger/10 border border-danger/30 text-danger text-xs flex items-start gap-2">
          <AlertTriangle className="w-4 h-4 flex-shrink-0 mt-0.5" />
          <span>{error}</span>
        </div>
      )}

      {initialLoading ? (
        <div className="space-y-2.5 animate-pulse py-2">
          <div className="h-4 bg-line/60 rounded w-3/4" />
          <div className="h-14 bg-line/40 rounded" />
          <div className="h-14 bg-line/40 rounded" />
        </div>
      ) : !analysis ? (
        <div className="p-5 text-center rounded bg-bg/50 border border-dashed border-line text-content-muted text-[11px] leading-relaxed">
          Click <span className="text-content font-semibold">Analyze with AI</span> to generate
          evidence-cited debugging notes and reproduction steps from this bug&apos;s sanitized timeline.
        </div>
      ) : analysis.status === 'FAILED' ? (
        <div className="p-3.5 rounded bg-danger/10 border border-danger/30 text-danger text-xs flex flex-col gap-1">
          <span className="font-bold">Analysis Failed ({analysis.provider})</span>
          <span className="text-content-muted">{analysis.errorMessage || 'Provider returned an error.'}</span>
        </div>
      ) : (
        <div className="flex flex-col gap-4">
          {/* 1. Summary */}
          {analysis.summary && (
            <div className="p-3 rounded bg-bg border border-line/70 flex flex-col gap-1">
              <span className="text-[10px] uppercase tracking-wider text-content-muted font-bold">
                Summary
              </span>
              <p className="text-xs text-content leading-relaxed font-sans text-pretty">
                {analysis.summary}
              </p>
            </div>
          )}

          {/* 2. Evidence (from recorded events) */}
          <div className="flex flex-col gap-2">
            <h3 className="text-[11px] uppercase tracking-wider font-bold text-accent flex items-center gap-1.5">
              <CheckCircle2 className="w-3.5 h-3.5" />
              Evidence (from recorded events)
            </h3>
            {analysis.evidence && analysis.evidence.length > 0 ? (
              <ul className="flex flex-col gap-2">
                {analysis.evidence.map((item, idx) => (
                  <li
                    key={idx}
                    className="p-2.5 rounded bg-bg/80 border border-accent/25 flex flex-col gap-2"
                  >
                    <p className="text-xs text-content leading-snug font-sans">{item.statement}</p>
                    <div className="flex flex-wrap items-center gap-1.5">
                      {(item.eventIds || []).map((evId) => (
                        <button
                          key={evId}
                          type="button"
                          onClick={() => onSelectEvidenceEvent?.(evId)}
                          title={`Jump to event ${evId} in timeline`}
                          className="inline-flex items-center gap-1 px-2 py-0.5 rounded bg-accent/10 hover:bg-accent/20 text-accent border border-accent/30 text-[10px] font-mono transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-accent"
                        >
                          <Link2 className="w-2.5 h-2.5" />
                          <span>{evId}</span>
                        </button>
                      ))}
                    </div>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="text-[11px] text-content-muted italic">
                No verified timeline event citations were returned.
              </p>
            )}
          </div>

          {/* 3. Hypotheses (AI guesses — verify) */}
          <div className="flex flex-col gap-2">
            <h3 className="text-[11px] uppercase tracking-wider font-bold text-warn flex items-center gap-1.5">
              <HelpCircle className="w-3.5 h-3.5" />
              Hypotheses (AI guesses — verify)
            </h3>
            {analysis.hypotheses && analysis.hypotheses.length > 0 ? (
              <ul className="flex flex-col gap-2">
                {analysis.hypotheses.map((h, idx) => (
                  <li
                    key={idx}
                    className="p-2.5 rounded bg-bg/70 border border-line flex flex-col gap-1.5"
                  >
                    <div className="flex items-start justify-between gap-2">
                      <span className="font-semibold text-content font-sans text-xs">
                        {h.cause}
                      </span>
                      <span
                        className={`px-1.5 py-0.5 rounded text-[9px] font-mono font-bold uppercase border flex-shrink-0 ${getConfidenceStyle(
                          h.confidence
                        )}`}
                      >
                        {h.confidence}
                      </span>
                    </div>
                    <p className="text-[11px] text-content-muted font-sans leading-relaxed">
                      {h.reasoning}
                    </p>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="text-[11px] text-content-muted italic">No hypotheses returned.</p>
            )}
          </div>

          {/* 4. Debugging Steps */}
          {analysis.debuggingSteps && analysis.debuggingSteps.length > 0 && (
            <div className="flex flex-col gap-1.5">
              <h3 className="text-[11px] uppercase tracking-wider font-bold text-content-muted flex items-center gap-1.5">
                <Wrench className="w-3.5 h-3.5" />
                Debugging Steps
              </h3>
              <ul className="list-disc pl-4 space-y-1 text-xs text-content/90 font-sans">
                {analysis.debuggingSteps.map((step, idx) => (
                  <li key={idx}>{step}</li>
                ))}
              </ul>
            </div>
          )}

          {/* 5. Reproduction Steps + Copy as Markdown */}
          {analysis.reproductionSteps && analysis.reproductionSteps.length > 0 && (
            <div className="flex flex-col gap-1.5">
              <div className="flex items-center justify-between gap-2">
                <h3 className="text-[11px] uppercase tracking-wider font-bold text-content-muted flex items-center gap-1.5">
                  <ListOrdered className="w-3.5 h-3.5" />
                  Reproduction Steps
                </h3>
                <button
                  type="button"
                  onClick={handleCopyReproMarkdown}
                  className="px-2 py-0.5 rounded bg-bg border border-line hover:border-accent text-[10px] text-content-muted hover:text-content flex items-center gap-1 transition-colors"
                >
                  {copiedRepro ? (
                    <>
                      <Check className="w-3 h-3 text-accent" />
                      <span className="text-accent">Copied MD</span>
                    </>
                  ) : (
                    <>
                      <Copy className="w-3 h-3" />
                      <span>Copy as Markdown</span>
                    </>
                  )}
                </button>
              </div>
              <ol className="list-decimal pl-4 space-y-1 text-xs text-content/90 font-sans">
                {analysis.reproductionSteps.map((step, idx) => (
                  <li key={idx}>{step}</li>
                ))}
              </ol>
            </div>
          )}

          {/* Provider Footer & Disclaimer */}
          <div className="pt-3 border-t border-line flex flex-col gap-1 text-[10px] text-content-muted">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <span>
                Provider: <span className="text-content">{analysis.provider}</span> · Model:{' '}
                <span className="text-content">{analysis.model}</span>
              </span>
              {analysis.createdAt && (
                <span className="tabular-nums">
                  {new Date(analysis.createdAt).toLocaleString()}
                </span>
              )}
            </div>
            <p className="italic opacity-80">
              Disclaimer: Evidence cites verified telemetry events; hypotheses are AI-generated guesses and must be verified before making code changes.
            </p>
          </div>
        </div>
      )}
    </section>
  );
}
