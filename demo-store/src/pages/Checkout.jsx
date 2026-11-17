import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useCart } from '../CartContext';
import { storeApi } from '../api';
import { Lock, AlertCircle, CheckCircle2, ArrowLeft } from 'lucide-react';

export default function Checkout() {
  const { items, coupon, subtotal, discount, total, clearCart } = useCart();

  const [email, setEmail] = useState('');
  const [fullName, setFullName] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [orderConfirmation, setOrderConfirmation] = useState(null);

  const handleCheckout = async (e) => {
    e.preventDefault();
    setError(null);
    setSubmitting(true);

    try {
      const result = await storeApi.checkout({
        items: items.map((i) => ({ id: i.id, qty: i.qty })),
        coupon: coupon?.code || null,
        customerEmail: email,
      });
      setOrderConfirmation(result);
      clearCart();
    } catch (err) {
      // Shopper-friendly error message (never expose raw stack traces to customers)
      setError(
        'We ran into an unexpected issue while placing your order. Your card was not charged — please try again or remove your promo code.'
      );
    } finally {
      setSubmitting(false);
    }
  };

  if (orderConfirmation) {
    return (
      <div className="max-w-lg mx-auto px-4 sm:px-6 py-16 text-center flex flex-col items-center gap-4">
        <div className="w-12 h-12 rounded-full bg-forest/10 text-forest flex items-center justify-center">
          <CheckCircle2 className="w-6 h-6" />
        </div>
        <h1 className="text-2xl font-bold text-ink">Order Confirmed</h1>
        <p className="text-sm text-stone">
          Thank you for your order. Your confirmation reference is{' '}
          <span className="font-mono font-semibold text-ink">{orderConfirmation.orderId}</span>.
        </p>
        <Link
          to="/"
          className="mt-2 px-5 py-2.5 rounded bg-ink text-canvas text-xs font-medium hover:bg-ink/90"
        >
          Back to Catalog
        </Link>
      </div>
    );
  }

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 py-10 flex flex-col gap-6">
      <Link
        to="/cart"
        className="inline-flex items-center gap-1.5 text-xs font-mono text-stone hover:text-ink w-fit"
      >
        <ArrowLeft className="w-3.5 h-3.5" />
        <span>Return to Bag</span>
      </Link>

      <h1 className="text-2xl font-bold tracking-tight text-ink pb-4 border-b border-border">
        Checkout
      </h1>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        {/* Customer Shipping Form */}
        <form
          onSubmit={handleCheckout}
          className="lg:col-span-7 bg-surface border border-border rounded p-6 flex flex-col gap-5"
        >
          <h2 className="text-sm font-semibold text-ink">Contact & Delivery</h2>

          <div className="flex flex-col gap-1.5">
            <label htmlFor="checkout-email" className="text-xs font-medium text-stone">
              Email Address
            </label>
            <input
              id="checkout-email"
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="alex@example.com"
              className="px-3 py-2 rounded bg-canvas border border-border text-sm text-ink"
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <label htmlFor="checkout-name" className="text-xs font-medium text-stone">
              Full Name
            </label>
            <input
              id="checkout-name"
              type="text"
              required
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              placeholder="Alex Rivera"
              className="px-3 py-2 rounded bg-canvas border border-border text-sm text-ink"
            />
          </div>

          {error && (
            <div
              role="alert"
              className="p-3.5 rounded bg-rust/10 border border-rust/30 text-rust text-xs flex items-start gap-2.5 leading-relaxed"
            >
              <AlertCircle className="w-4 h-4 flex-shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          <button
            type="submit"
            disabled={submitting || items.length === 0}
            data-bugdna-action="CHECKOUT"
            className="mt-2 w-full py-3 px-4 rounded bg-forest text-white text-sm font-semibold flex items-center justify-center gap-2 hover:bg-forest/90 disabled:opacity-50 transition-colors"
          >
            <Lock className="w-4 h-4" />
            <span>{submitting ? 'Processing Order...' : `Place Order — $${total.toFixed(2)}`}</span>
          </button>
        </form>

        {/* Order Summary */}
        <aside className="lg:col-span-5 bg-surface border border-border rounded p-6 h-fit flex flex-col gap-4">
          <h2 className="text-sm font-semibold text-ink pb-3 border-b border-border">
            Order Summary
          </h2>

          <div className="flex flex-col gap-2.5 text-sm">
            {items.map((item) => (
              <div key={item.id} className="flex justify-between gap-2">
                <span className="text-stone">
                  {item.name} × {item.qty}
                </span>
                <span className="font-mono tabular-nums text-ink">
                  ${(item.price * item.qty).toFixed(2)}
                </span>
              </div>
            ))}
          </div>

          <div className="pt-3 border-t border-border space-y-1.5 text-sm">
            <div className="flex justify-between text-stone">
              <span>Subtotal</span>
              <span className="font-mono tabular-nums">${subtotal.toFixed(2)}</span>
            </div>
            {coupon && (
              <div className="flex justify-between text-forest">
                <span>Promo ({coupon.code})</span>
                <span className="font-mono tabular-nums">−${discount.toFixed(2)}</span>
              </div>
            )}
            <div className="flex justify-between font-bold text-ink pt-2 border-t border-border text-base">
              <span>Total</span>
              <span className="font-mono tabular-nums">${total.toFixed(2)}</span>
            </div>
          </div>
        </aside>
      </div>
    </div>
  );
}
