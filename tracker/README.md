# BugDNA Browser Tracker (`bugdna-tracker.js`)

Zero-dependency ES module for privacy-first error tracking and click-to-crash timeline collection.

## Embed in Any App (3 Lines)

```js
import { BugDNA } from './bugdna-tracker.js';

BugDNA.init({
  endpoint: 'http://localhost:8080/api/v1/ingest/events',
  projectKey: 'demo-store-dev-key',
});
```

## Tagging User Actions (Privacy-First)

The tracker **never** records input values, cookies, request bodies, or arbitrary DOM text. To include a meaningful click in the debugging timeline, add `data-bugdna-action`:

```html
<button data-bugdna-action="APPLY_COUPON">Apply Coupon</button>
<button data-bugdna-action="CHECKOUT">Complete Order</button>
```

## What It Captures
- `PAGE_VIEW` & `ROUTE_CHANGE` (normalized paths, query strings stripped)
- `CLICK` (strictly elements with `data-bugdna-action`)
- `JS_ERROR` & `PROMISE_REJECTION` (top 3 sanitized stack frames)
- `API_ERROR` (checks `response.ok` because `fetch()` does not reject on HTTP 4xx/5xx)
- `NETWORK_ERROR` (catches and rethrows network failures)

## Running Tracker Unit Tests

```bash
npm install
npm test
```

