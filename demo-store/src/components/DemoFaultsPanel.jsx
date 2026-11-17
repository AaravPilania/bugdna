import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { X, AlertTriangle, PackageSearch, Heart, Tag, FileWarning } from 'lucide-react';

export default function DemoFaultsPanel({ isOpen, onClose }) {
  const navigate = useNavigate();
  const [notice, setNotice] = useState(null);

  if (!isOpen) return null;

  const triggerTrackOrderTypeError = () => {
    setNotice('Unable to display order tracking right now. Our team has been notified.');
    // Throw asynchronously so window 'error' event fires and BugDNA tracker captures the exact TypeError
    setTimeout(function trackOrderStatus() {
      const order = { id: 'ORD-1042', item: undefined };
      // Intentional fault (b): TypeError reading 'price' of undefined
      return order.item.price;
    }, 0);
  };

  const triggerWishlistRejection = () => {
    setNotice('Wishlist sync is temporarily unavailable. Please try again shortly.');
    // Intentional fault (d): Unhandled Promise Rejection
    Promise.reject(new Error('Wishlist sync failed with status 503'));
  };

  const goToMissingProduct = () => {
    setNotice(null);
    onClose();
    navigate('/product/999');
  };

  const goToCouponFlow = () => {
    setNotice(null);
    onClose();
    navigate('/cart');
  };

  return (
    <div className="border-b border-border bg-surface shadow-sm">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 py-4 flex flex-col gap-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 text-rust" />
            <h2 className="text-xs font-mono uppercase tracking-wider font-semibold text-ink">
              Presenter Demo Faults (Tracked by BugDNA)
            </h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close Demo Faults"
            className="p-1 text-stone hover:text-ink rounded"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 text-xs">
          {/* Fault A: SAVE20 */}
          <button
            type="button"
            onClick={goToCouponFlow}
            data-bugdna-action="DEMO_FAULT_SAVE20_FLOW"
            className="p-3 rounded border border-border bg-canvas/60 hover:border-ink text-left flex flex-col gap-1 transition-colors"
          >
            <span className="font-mono font-semibold text-ink flex items-center gap-1.5">
              <Tag className="w-3.5 h-3.5 text-forest" />
              a) Coupon SAVE20 → 500
            </span>
            <span className="text-stone leading-snug">
              Go to Cart, apply <code className="font-mono font-semibold text-ink">SAVE20</code>, then click Checkout.
            </span>
          </button>

          {/* Fault B: TypeError */}
          <button
            type="button"
            onClick={triggerTrackOrderTypeError}
            data-bugdna-action="TRACK_ORDER"
            className="p-3 rounded border border-border bg-canvas/60 hover:border-ink text-left flex flex-col gap-1 transition-colors"
          >
            <span className="font-mono font-semibold text-ink flex items-center gap-1.5">
              <PackageSearch className="w-3.5 h-3.5 text-rust" />
              b) Track Order (TypeError)
            </span>
            <span className="text-stone leading-snug">
              Throws uncaught TypeError reading &apos;price&apos; of undefined.
            </span>
          </button>

          {/* Fault C: 404 */}
          <button
            type="button"
            onClick={goToMissingProduct}
            data-bugdna-action="OPEN_MISSING_PRODUCT"
            className="p-3 rounded border border-border bg-canvas/60 hover:border-ink text-left flex flex-col gap-1 transition-colors"
          >
            <span className="font-mono font-semibold text-ink flex items-center gap-1.5">
              <FileWarning className="w-3.5 h-3.5 text-amber-700" />
              c) Product #999 (404)
            </span>
            <span className="text-stone leading-snug">
              Opens /product/999 triggering GET 404 PRODUCT_NOT_FOUND.
            </span>
          </button>

          {/* Fault D: Promise Rejection */}
          <button
            type="button"
            onClick={triggerWishlistRejection}
            data-bugdna-action="SYNC_WISHLIST"
            className="p-3 rounded border border-border bg-canvas/60 hover:border-ink text-left flex flex-col gap-1 transition-colors"
          >
            <span className="font-mono font-semibold text-ink flex items-center gap-1.5">
              <Heart className="w-3.5 h-3.5 text-rust" />
              d) Sync Wishlist (Rejection)
            </span>
            <span className="text-stone leading-snug">
              Fires an unhandled promise rejection (status 503).
            </span>
          </button>
        </div>

        {notice && (
          <div className="p-2.5 rounded bg-rust/10 border border-rust/30 text-rust text-xs font-medium">
            {notice}
          </div>
        )}
      </div>
    </div>
  );
}

