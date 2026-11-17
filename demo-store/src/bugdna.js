import { BugDNA } from '../../tracker/bugdna-tracker.js';

const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080';
const PROJECT_KEY = import.meta.env.VITE_BUGDNA_PROJECT_KEY || 'demo-store-dev-key';

BugDNA.init({
  endpoint: `${API_BASE}/api/v1/ingest/events`,
  projectKey: PROJECT_KEY,
  maxBuffer: 30,
  flushIntervalMs: 10000,
});

export { BugDNA };

