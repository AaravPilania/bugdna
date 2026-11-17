import { describe, it, expect, vi } from 'vitest';
import { sanitizePath, sanitizeText, createFetchWrapper } from './bugdna-tracker.js';

describe('BugDNA Tracker sanitizePath()', () => {
  it('strips query strings and hashes and normalizes numeric and UUID segments to :id', () => {
    expect(sanitizePath('/product/999?promo=SAVE20#reviews')).toBe('/product/:id');
    expect(sanitizePath('http://localhost:8080/demo-api/products/42?token=secret')).toBe('/demo-api/products/:id');
    expect(sanitizePath('/orders/a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d/receipt?x=1')).toBe('/orders/:id/receipt');
    expect(sanitizePath('/checkout/')).toBe('/checkout');
  });
});

describe('BugDNA Tracker sanitizeText()', () => {
  it('masks emails, Bearer tokens, long hex tokens, and digit runs >= 8', () => {
    const raw = 'Checkout error for alice@example.com card 4111222233334444 with Bearer eyJhbGciOiJIUzI1NiJ9.payload';
    const result = sanitizeText(raw);

    expect(result).not.toContain('alice@example.com');
    expect(result).toContain('[email]');
    expect(result).not.toContain('4111222233334444');
    expect(result).toContain('[number]');
    expect(result).not.toContain('eyJhbGciOiJIUzI1NiJ9');
    expect(result).toContain('Bearer [token]');
  });

  it('truncates long messages to 300 characters', () => {
    const longMsg = 'A'.repeat(450);
    expect(sanitizeText(longMsg).length).toBe(300);
  });
});

describe('BugDNA Tracker fetch wrapper', () => {
  const INGEST_URL = 'http://localhost:8080/api/v1/ingest/events';

  it('records API_ERROR on HTTP 500 (since fetch does not reject) and still returns response to caller', async () => {
    const recorded = [];
    const mockRes = new Response(
      JSON.stringify({
        errorCode: 'DEMO_COUPON_RULE_ERROR',
        message: 'Coupon rule evaluation failed',
      }),
      {
        status: 500,
        statusText: 'Internal Server Error',
        headers: { 'Content-Type': 'application/json' },
      }
    );

    const origFetch = vi.fn().mockResolvedValue(mockRes);
    const wrapped = createFetchWrapper(origFetch, INGEST_URL, (ev, flush) => {
      recorded.push({ ev, flush });
    });

    const returnedRes = await wrapped('http://localhost:8080/demo-api/checkout', { method: 'POST' });

    expect(returnedRes.status).toBe(500);
    expect(returnedRes.ok).toBe(false);
    const body = await returnedRes.json();
    expect(body.errorCode).toBe('DEMO_COUPON_RULE_ERROR');

    expect(recorded.length).toBe(1);
    expect(recorded[0].flush).toBe(true);
    expect(recorded[0].ev.type).toBe('API_ERROR');
    expect(recorded[0].ev.httpMethod).toBe('POST');
    expect(recorded[0].ev.httpStatus).toBe(500);
    expect(recorded[0].ev.endpoint).toBe('/demo-api/checkout');
    expect(recorded[0].ev.errorCode).toBe('DEMO_COUPON_RULE_ERROR');
  });

  it('records NETWORK_ERROR on fetch throw and rethrows error to caller', async () => {
    const recorded = [];
    const origFetch = vi.fn().mockRejectedValue(new TypeError('Failed to fetch'));
    const wrapped = createFetchWrapper(origFetch, INGEST_URL, (ev, flush) => {
      recorded.push({ ev, flush });
    });

    await expect(wrapped('http://localhost:8080/demo-api/products')).rejects.toThrow('Failed to fetch');

    expect(recorded.length).toBe(1);
    expect(recorded[0].flush).toBe(true);
    expect(recorded[0].ev.type).toBe('NETWORK_ERROR');
    expect(recorded[0].ev.endpoint).toBe('/demo-api/products');
  });

  it('never tracks calls to its own ingest endpoint (prevents infinite loops)', async () => {
    const recorded = [];
    const mockErrRes = new Response('Unauthorized', { status: 401 });
    const origFetch = vi.fn().mockResolvedValue(mockErrRes);
    const wrapped = createFetchWrapper(origFetch, INGEST_URL, (ev, flush) => {
      recorded.push({ ev, flush });
    });

    await wrapped(INGEST_URL, { method: 'POST' });
    expect(recorded.length).toBe(0);
  });

  it('does not record API_ERROR when response.ok is true (HTTP 200)', async () => {
    const recorded = [];
    const mockOkRes = new Response(JSON.stringify({ ok: true }), { status: 200 });
    const origFetch = vi.fn().mockResolvedValue(mockOkRes);
    const wrapped = createFetchWrapper(origFetch, INGEST_URL, (ev, flush) => {
      recorded.push({ ev, flush });
    });

    const res = await wrapped('http://localhost:8080/demo-api/products');
    expect(res.ok).toBe(true);
    expect(recorded.length).toBe(0);
  });
});

