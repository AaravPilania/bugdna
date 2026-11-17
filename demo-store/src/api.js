const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080';

export const storeApi = {
  getProducts: async () => {
    const res = await fetch(`${API_BASE}/demo-api/products`);
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || `Failed to load products (HTTP ${res.status})`);
    }
    return res.json();
  },

  getProduct: async (id) => {
    const res = await fetch(`${API_BASE}/demo-api/products/${id}`);
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || `Product #${id} could not be found`);
    }
    return res.json();
  },

  applyCoupon: async (code) => {
    const res = await fetch(`${API_BASE}/demo-api/cart/coupon`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ code }),
    });
    const data = await res.json().catch(() => ({}));
    if (!res.ok) {
      throw new Error(data.message || 'Invalid coupon code');
    }
    return data;
  },

  checkout: async ({ items, coupon, customerEmail }) => {
    const res = await fetch(`${API_BASE}/demo-api/checkout`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ items, coupon, customerEmail }),
    });
    const data = await res.json().catch(() => ({}));
    if (!res.ok) {
      throw new Error(
        data.message || 'We ran into a problem processing your checkout. Please try again.'
      );
    }
    return data;
  },
};

