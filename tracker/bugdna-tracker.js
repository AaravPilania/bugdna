/**
 * BugDNA Zero-Dependency Browser Telemetry Tracker (ES Module)
 *
 * Privacy-first design:
 *  - Never collects input values, passwords, cookies, auth headers, request/response bodies, or query strings.
 *  - Captures clicks ONLY on elements carrying explicit `data-bugdna-action="..."` attributes.
 *  - Masks emails, Bearer/long tokens, and digit runs >= 8 before buffering or transmission.
 */

const SESSION_KEY = 'bugdna_session_id';
const RETRY_QUEUE_KEY = 'bugdna_retry_queue';

const EMAIL_RE = /\b[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}\b/g;
const BEARER_RE = /Bearer\s+[A-Za-z0-9._\-~+/]+=*/gi;
const LONG_TOKEN_RE = /\b[0-9a-fA-F]{32,64}\b/g;
const DIGIT_RUN_RE = /\b\d{8,}\b/g;
const UUID_PATH_RE = /\/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}(?=\/|$)/g;
const NUMERIC_PATH_RE = /\/\d+(?=\/|$)/g;

/**
 * Generates a RFC 4122 UUIDv4 using Web Crypto API with safe fallback.
 */
function generateUUID() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

/**
 * Strips query strings and hashes, and normalizes numeric/UUID path segments to `:id`.
 */
export function sanitizePath(rawUrl) {
  if (!rawUrl || typeof rawUrl !== 'string') return '/';
  let path = rawUrl.trim();
  try {
    // Handle full URLs by extracting pathname only (drops origin, query string, and hash)
    if (path.startsWith('http://') || path.startsWith('https://')) {
      const parsed = new URL(path);
      path = parsed.pathname;
    } else {
      const qIdx = path.indexOf('?');
      if (qIdx >= 0) path = path.slice(0, qIdx);
      const hIdx = path.indexOf('#');
      if (hIdx >= 0) path = path.slice(0, hIdx);
    }
  } catch {
    const qIdx = path.indexOf('?');
    if (qIdx >= 0) path = path.slice(0, qIdx);
  }
  path = path.replace(UUID_PATH_RE, '/:id');
  path = path.replace(NUMERIC_PATH_RE, '/:id');
  path = path.replace(/\/+/g, '/');
  if (path.length > 1 && path.endsWith('/')) {
    path = path.slice(0, -1);
  }
  return path || '/';
}

/**
 * Masks PII (emails, tokens, long digit runs) and truncates strings to maxLength (default 300).
 */
export function sanitizeText(text, maxLength = 300) {
  if (text == null) return null;
  let str = String(text).trim();
  if (!str) return null;

  str = str.replace(EMAIL_RE, '[email]');
  str = str.replace(BEARER_RE, 'Bearer [token]');
  str = str.replace(LONG_TOKEN_RE, '[token]');
  str = str.replace(DIGIT_RUN_RE, '[number]');

  if (str.length > maxLength) {
    str = str.slice(0, maxLength);
  }
  return str;
}

/**
 * Extracts and sanitizes the first 3 stack frames from an Error stack trace.
 */
export function extractTopFrames(stack, maxFrames = 3) {
  if (!stack || typeof stack !== 'string') return null;
  const lines = stack
    .split('\n')
    .map((l) => l.trim())
    .filter((l) => l && !l.startsWith('Error') && !l.startsWith('TypeError') && !l.startsWith('ReferenceError'));
  return sanitizeText(lines.slice(0, maxFrames).join(' | '), 450);
}

/**
 * Wraps a fetch implementation to record API_ERROR (when !res.ok) and NETWORK_ERROR (on throw).
 * Why: fetch() does NOT reject on HTTP 4xx/5xx responses; it only rejects on network failures.
 * Never tracks calls to the BugDNA ingest endpoint itself to avoid feedback loops.
 */
export function createFetchWrapper(origFetch, ingestEndpoint, recordFn) {
  return async function wrappedFetch(input, init) {
    const urlStr = typeof input === 'string' ? input : input instanceof URL ? input.href : input?.url || '';
    const method = (init?.method || (typeof input !== 'string' && input?.method) || 'GET').toUpperCase();

    // Never track calls to BugDNA's own ingest endpoint (prevents infinite telemetry loops)
    if (ingestEndpoint && urlStr.includes(ingestEndpoint)) {
      return origFetch(input, init);
    }

    try {
      const response = await origFetch(input, init);

      // fetch() resolves on HTTP 4xx/5xx — we MUST check response.ok explicitly
      if (!response.ok) {
        let errorCode = null;
        let errorMessage = `HTTP ${response.status} ${response.statusText || ''}`.trim();

        try {
          const cloned = response.clone();
          const data = await cloned.json();
          if (data && typeof data === 'object') {
            errorCode = data.errorCode || data.code || null;
            if (data.message || data.detail) {
              errorMessage = data.message || data.detail;
            }
          }
        } catch {
          // Non-JSON error body is fine; fall back to status text
        }

        recordFn({
          type: 'API_ERROR',
          httpMethod: method,
          httpStatus: response.status,
          endpoint: sanitizePath(urlStr),
          errorCode: sanitizeText(errorCode, 80),
          message: sanitizeText(errorMessage, 300),
        }, true);
      }

      return response;
    } catch (err) {
      // Network failure (DNS, offline, CORS block)
      recordFn({
        type: 'NETWORK_ERROR',
        httpMethod: method,
        endpoint: sanitizePath(urlStr),
        message: sanitizeText(err?.message || 'Network request failed', 300),
      }, true);
      throw err;
    }
  };
}

class BugDNATracker {
  constructor() {
    this.initialized = false;
    this.config = {
      endpoint: '',
      projectKey: '',
      maxBuffer: 30,
      flushIntervalMs: 10000,
    };
    this.sessionId = null;
    this.buffer = [];
    this.flushTimer = null;
    this.origFetch = null;
    this.retryAttempt = 0;
  }

  init({ endpoint, projectKey, maxBuffer = 30, flushIntervalMs = 10000 } = {}) {
    if (this.initialized) return;
    if (!endpoint || !projectKey) {
      console.warn('[BugDNA] init requires endpoint and projectKey.');
      return;
    }

    this.config = { endpoint, projectKey, maxBuffer, flushIntervalMs };
    this.sessionId = this._getOrCreateSessionId();
    this.initialized = true;

    this._installNavigationListeners();
    this._installClickListener();
    this._installErrorListeners();
    this._installFetchWrapper();
    this._installVisibilityFlush();

    // Flush periodically
    this.flushTimer = setInterval(() => this.flush(), this.config.flushIntervalMs);

    // Retry any unsent batch from previous page unload
    this._drainRetryQueue();

    // Record initial page view
    this.record({
      type: 'PAGE_VIEW',
      pagePath: sanitizePath(window.location.pathname),
    });
  }

  _getOrCreateSessionId() {
    try {
      let id = sessionStorage.getItem(SESSION_KEY);
      if (!id) {
        id = generateUUID();
        sessionStorage.setItem(SESSION_KEY, id);
      }
      return id;
    } catch {
      return generateUUID();
    }
  }

  record(partialEvent, flushImmediately = false) {
    if (!this.initialized) return;

    const event = {
      eventId: generateUUID(),
      type: partialEvent.type,
      actionLabel: sanitizeText(partialEvent.actionLabel, 80),
      pagePath: partialEvent.pagePath || sanitizePath(window.location.pathname),
      httpMethod: partialEvent.httpMethod || null,
      httpStatus: partialEvent.httpStatus || null,
      endpoint: partialEvent.endpoint || null,
      errorCode: sanitizeText(partialEvent.errorCode, 80),
      errorName: sanitizeText(partialEvent.errorName, 80),
      message: sanitizeText(partialEvent.message, 300),
      stackTop: partialEvent.stackTop || null,
      occurredAt: new Date().toISOString(),
    };

    // Ring buffer: keep at most maxBuffer events
    if (this.buffer.length >= this.config.maxBuffer) {
      this.buffer.shift();
    }
    this.buffer.push(event);

    if (flushImmediately) {
      this.flush();
    }
  }

  _installNavigationListeners() {
    const recordRoute = () => {
      this.record({
        type: 'ROUTE_CHANGE',
        pagePath: sanitizePath(window.location.pathname),
      });
    };

    const origPushState = history.pushState;
    history.pushState = (...args) => {
      origPushState.apply(history, args);
      recordRoute();
    };

    const origReplaceState = history.replaceState;
    history.replaceState = (...args) => {
      origReplaceState.apply(history, args);
      recordRoute();
    };

    window.addEventListener('popstate', recordRoute);
  }

  _installClickListener() {
    document.addEventListener(
      'click',
      (e) => {
        // Privacy rule: ONLY track clicks on elements explicitly tagged with data-bugdna-action
        const target = e.target?.closest?.('[data-bugdna-action]');
        if (!target) return;

        const actionLabel = target.getAttribute('data-bugdna-action');
        if (!actionLabel) return;

        this.record({
          type: 'CLICK',
          actionLabel: sanitizeText(actionLabel, 80),
          pagePath: sanitizePath(window.location.pathname),
        });
      },
      { capture: true }
    );
  }

  _installErrorListeners() {
    window.addEventListener('error', (event) => {
      const err = event.error;
      this.record(
        {
          type: 'JS_ERROR',
          errorName: err?.name || 'Error',
          message: sanitizeText(err?.message || event.message || 'Uncaught JavaScript error', 300),
          stackTop: extractTopFrames(err?.stack || `${event.filename || ''}:${event.lineno || 0}:${event.colno || 0}`),
        },
        true
      );
    });

    window.addEventListener('unhandledrejection', (event) => {
      const reason = event.reason;
      const name = reason?.name || 'UnhandledPromiseRejection';
      const msg = reason?.message || (typeof reason === 'string' ? reason : 'Promise rejected');
      this.record(
        {
          type: 'PROMISE_REJECTION',
          errorName: name,
          message: sanitizeText(msg, 300),
          stackTop: extractTopFrames(reason?.stack),
        },
        true
      );
    });
  }

  _installFetchWrapper() {
    if (typeof window === 'undefined' || typeof window.fetch !== 'function') return;
    this.origFetch = window.fetch.bind(window);
    window.fetch = createFetchWrapper(
      this.origFetch,
      this.config.endpoint,
      (ev, flushNow) => this.record(ev, flushNow)
    );
  }

  _installVisibilityFlush() {
    document.addEventListener('visibilitychange', () => {
      if (document.visibilityState === 'hidden') {
        this.flush();
      }
    });
  }

  async flush() {
    if (!this.initialized || this.buffer.length === 0) return;

    const batch = this.buffer.splice(0, this.buffer.length);
    const payload = {
      sessionId: this.sessionId,
      userAgent: sanitizeText(navigator.userAgent, 255),
      events: batch,
    };

    await this._sendBatch(payload);
  }

  async _sendBatch(payload, attempt = 0) {
    const fetchFn = this.origFetch || window.fetch.bind(window);

    try {
      const res = await fetchFn(this.config.endpoint, {
        method: 'POST',
        keepalive: true,
        headers: {
          'Content-Type': 'application/json',
          'X-BugDNA-Key': this.config.projectKey,
        },
        body: JSON.stringify(payload),
      });

      if (!res.ok) {
        throw new Error(`Ingest HTTP ${res.status}`);
      }

      // Clear any persisted retry batch on success
      try {
        sessionStorage.removeItem(RETRY_QUEUE_KEY);
      } catch {}
    } catch {
      // Persist to sessionStorage and retry with exponential backoff (2s, 4s, 8s, max 3)
      if (attempt < 3) {
        const delayMs = Math.pow(2, attempt + 1) * 1000;
        try {
          sessionStorage.setItem(RETRY_QUEUE_KEY, JSON.stringify(payload));
        } catch {}
        setTimeout(() => this._sendBatch(payload, attempt + 1), delayMs);
      }
    }
  }

  _drainRetryQueue() {
    try {
      const raw = sessionStorage.getItem(RETRY_QUEUE_KEY);
      if (raw) {
        const payload = JSON.parse(raw);
        sessionStorage.removeItem(RETRY_QUEUE_KEY);
        if (payload?.events?.length > 0) {
          this._sendBatch(payload, 0);
        }
      }
    } catch {}
  }
}

export const BugDNA = new BugDNATracker();
export default BugDNA;

