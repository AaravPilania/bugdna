import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { ShoppingBag, SlidersHorizontal } from 'lucide-react';
import { useCart } from '../CartContext';

export default function StoreHeader({ onToggleFaults, faultsOpen }) {
  const { totalCount } = useCart();
  const location = useLocation();

  return (
    <header className="sticky top-0 z-30 bg-canvas/90 backdrop-blur-sm border-b border-border">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 h-16 flex items-center justify-between">
        <div className="flex items-center gap-8">
          <Link to="/" className="flex items-baseline gap-2">
            <span className="text-lg font-bold tracking-tight text-ink">KOVA GOODS</span>
            <span className="text-[11px] font-mono uppercase tracking-wider text-stone hidden sm:inline">
              Field Supply
            </span>
          </Link>

          <nav className="hidden md:flex items-center gap-6 text-sm text-stone">
            <Link
              to="/"
              className={`hover:text-ink transition-colors ${location.pathname === '/' ? 'text-ink font-medium' : ''}`}
            >
              Catalog
            </Link>
            <Link
              to="/cart"
              className={`hover:text-ink transition-colors ${location.pathname === '/cart' ? 'text-ink font-medium' : ''}`}
            >
              Cart
            </Link>
            <Link
              to="/checkout"
              className={`hover:text-ink transition-colors ${location.pathname === '/checkout' ? 'text-ink font-medium' : ''}`}
            >
              Checkout
            </Link>
          </nav>
        </div>

        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={onToggleFaults}
            aria-label="Toggle Demo Faults Panel"
            className={`px-3 py-1.5 rounded text-xs font-mono border transition-colors flex items-center gap-1.5 ${
              faultsOpen
                ? 'bg-ink text-canvas border-ink'
                : 'bg-surface text-stone border-border hover:border-ink hover:text-ink'
            }`}
          >
            <SlidersHorizontal className="w-3.5 h-3.5" />
            <span>Demo faults</span>
          </button>

          <Link
            to="/cart"
            data-bugdna-action="OPEN_CART"
            className="relative px-3.5 py-1.5 rounded bg-forest text-white text-sm font-medium flex items-center gap-2 hover:bg-forest/90 transition-colors"
          >
            <ShoppingBag className="w-4 h-4" />
            <span>Cart</span>
            <span className="font-mono text-xs bg-white/20 px-1.5 py-0.5 rounded tabular-nums">
              {totalCount}
            </span>
          </Link>
        </div>
      </div>
    </header>
  );
}

