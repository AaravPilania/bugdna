import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useCart } from '../CartContext';
import { storeApi } from '../api';
import { Trash2, Tag, ArrowRight, CheckCircle2, AlertCircle } from 'lucide-react';

export default function Cart() {
  const navigate = useNavigate();
  const { items, coupon, setCoupon, updateQty, removeItem, subtotal, discount, total } = useCart();

  const [codeInput, setCodeInput] = useState(coupon?.code || '');
  const [couponLoading, setCouponLoading] = useState(false);
  const [couponError, setCouponError] = useState(null);
  const [couponSuccess, setCouponSuccess] = useState(null);

  const handleApplyCoupon = async (e) => {
    e.preventDefault();
    if (!codeInput.trim()) return;
    setCouponError(null);
    setCouponSuccess(null);
    setCouponLoading(true);

    try {
      const result = await storeApi.applyCoupon(codeInput.trim());
      setCoupon({ code: result.code, discountPercent: result.discountPercent });
      setCouponSuccess(result.message || `Coupon ${result.code} applied!`);
    } catch (err) {
      setCoupon(null);
      setCouponError(err.message || 'That promo code is not valid.');
    } finally {
      setCouponLoading(false);
    }
  };

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 py-10 flex flex-col gap-8">
      <div className="pb-4 border-b border-border flex items-baseline justify-between">
        <h1 className="text-2xl font-bold tracking-tight text-ink">Shopping Bag</h1>
        <span className="text-xs font-mono text-stone tabular-nums">
          {items.length} line item{items.length === 1 ? '' : 's'}
        </span>
      </div>

      {items.length === 0 ? (
        <div className="p-12 bg-surface border border-border rounded text-center flex flex-col items-center gap-3">
          <p className="text-sm text-stone">Your bag is currently empty.</p>
          <Link
            to="/"
            className="px-4 py-2 rounded bg-ink text-canvas text-xs font-medium hover:bg-ink/90"
          >
            Continue Shopping
          </Link>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
          {/* Items List */}
          <div className="lg:col-span-7 flex flex-col gap-3">
            {items.map((item) => (
              <div
                key={item.id}
                className="p-4 bg-surface border border-border rounded flex items-center justify-between gap-4"
              >
                <div className="flex flex-col gap-0.5">
                  <Link
                    to={`/product/${item.id}`}
                    className="text-sm font-semibold text-ink hover:text-forest"
                  >
                    {item.name}
                  </Link>
                  <span className="text-xs font-mono text-stone tabular-nums">
                    ${item.price.toFixed(2)} each
                  </span>
                </div>

                <div className="flex items-center gap-3">
                  <div className="flex items-center border border-border rounded bg-canvas text-xs font-mono">
                    <button
                      type="button"
                      onClick={() => updateQty(item.id, -1)}
                      aria-label={`Decrease quantity for ${item.name}`}
                      className="px-2.5 py-1 hover:bg-border/40"
                    >
                      −
                    </button>
                    <span className="px-2 tabular-nums">{item.qty}</span>
                    <button
                      type="button"
                      onClick={() => updateQty(item.id, 1)}
                      aria-label={`Increase quantity for ${item.name}`}
                      className="px-2.5 py-1 hover:bg-border/40"
                    >
                      +
                    </button>
                  </div>

                  <span className="w-16 text-right text-sm font-mono font-semibold tabular-nums">
                    ${(item.price * item.qty).toFixed(2)}
                  </span>

                  <button
                    type="button"
                    onClick={() => removeItem(item.id)}
                    aria-label={`Remove ${item.name}`}
                    className="p-1 text-stone hover:text-rust transition-colors"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            ))}
          </div>

          {/* Summary & Coupon Box */}
          <div className="lg:col-span-5 flex flex-col gap-4">
            <div className="p-5 bg-surface border border-border rounded flex flex-col gap-4">
              <h2 className="text-sm font-semibold text-ink">Promo Code</h2>
              <form onSubmit={handleApplyCoupon} className="flex gap-2">
                <div className="relative flex-1">
                  <Tag className="w-3.5 h-3.5 text-stone absolute left-3 top-2.5 pointer-events-none" />
                  <input
                    type="text"
                    value={codeInput}
                    onChange={(e) => setCodeInput(e.target.value)}
                    placeholder="Try SAVE20"
                    aria-label="Coupon code"
                    className="w-full pl-8 pr-3 py-2 rounded bg-canvas border border-border text-xs font-mono uppercase text-ink placeholder:normal-case placeholder:text-stone/60"
                  />
                </div>
                <button
                  type="submit"
                  disabled={couponLoading}
                  data-bugdna-action="APPLY_COUPON"
                  className="px-4 py-2 rounded bg-ink text-canvas text-xs font-medium hover:bg-ink/90 disabled:opacity-50 transition-colors"
                >
                  {couponLoading ? 'Checking...' : 'Apply'}
                </button>
              </form>

              {couponSuccess && (
                <div className="p-2.5 rounded bg-forest/10 border border-forest/30 text-forest text-xs flex items-center gap-2">
                  <CheckCircle2 className="w-4 h-4 flex-shrink-0" />
                  <span>{couponSuccess}</span>
                </div>
              )}

              {couponError && (
                <div className="p-2.5 rounded bg-rust/10 border border-rust/30 text-rust text-xs flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 flex-shrink-0" />
                  <span>{couponError}</span>
                </div>
              )}

              <div className="pt-4 border-t border-border space-y-2 text-sm">
                <div className="flex justify-between text-stone">
                  <span>Subtotal</span>
                  <span className="font-mono tabular-nums">${subtotal.toFixed(2)}</span>
                </div>
                {discount > 0 && (
                  <div className="flex justify-between text-forest">
                    <span>Discount ({coupon.code})</span>
                    <span className="font-mono tabular-nums">−${discount.toFixed(2)}</span>
                  </div>
                )}
                <div className="flex justify-between font-bold text-ink pt-2 border-t border-border text-base">
                  <span>Estimated Total</span>
                  <span className="font-mono tabular-nums">${total.toFixed(2)}</span>
                </div>
              </div>

              <button
                type="button"
                data-bugdna-action="PROCEED_TO_CHECKOUT"
                onClick={() => navigate('/checkout')}
                className="mt-2 w-full py-2.5 px-4 rounded bg-forest text-white text-sm font-medium flex items-center justify-center gap-2 hover:bg-forest/90 transition-colors"
              >
                <span>Proceed to Checkout</span>
                <ArrowRight className="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
