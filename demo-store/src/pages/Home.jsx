import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { storeApi } from '../api';
import { useCart } from '../CartContext';
import { Plus, ArrowRight, Check } from 'lucide-react';

const PRODUCT_SWATCHES = {
  1: { bg: '#EAE6DD', label: 'Natural Canvas' },
  2: { bg: '#DFE3DE', label: 'Sage Fleece' },
  3: { bg: '#E3E5E8', label: 'Brushed Steel' },
  4: { bg: '#DCDAD3', label: 'Ballistic Olive' },
};

export default function Home() {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [addedId, setAddedId] = useState(null);
  const { addItem } = useCart();

  useEffect(() => {
    storeApi
      .getProducts()
      .then((data) => setProducts(data || []))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  const handleAdd = (product) => {
    addItem(product);
    setAddedId(product.id);
    setTimeout(() => setAddedId(null), 1200);
  };

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 py-10 flex flex-col gap-10">
      {/* Editorial Intro */}
      <section className="flex flex-col md:flex-row md:items-end justify-between gap-4 pb-8 border-b border-border">
        <div className="max-w-xl">
          <p className="text-xs font-mono uppercase tracking-widest text-forest mb-2">
            Autumn / Field Collection
          </p>
          <h1 className="text-3xl sm:text-4xl font-bold tracking-tight text-ink">
            Durable essentials for daily transit.
          </h1>
          <p className="text-sm text-stone mt-2 leading-relaxed">
            Small-batch footwear, heavyweight cotton, and waterproof carry built to outlast trends. Use code <span className="font-mono font-semibold text-ink">SAVE20</span> in your cart for 20% off.
          </p>
        </div>
        <Link
          to="/cart"
          className="inline-flex items-center gap-2 text-sm font-medium text-forest hover:underline"
        >
          <span>View your bag</span>
          <ArrowRight className="w-4 h-4" />
        </Link>
      </section>

      {error && (
        <div className="p-4 rounded bg-rust/10 border border-rust/30 text-rust text-sm">
          We couldn&apos;t load the catalog right now. Please refresh in a moment.
        </div>
      )}

      {loading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          {[1, 2, 3, 4].map((n) => (
            <div key={n} className="h-80 rounded bg-border/40 animate-pulse" />
          ))}
        </div>
      ) : (
        <section className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          {products.map((product) => {
            const swatch = PRODUCT_SWATCHES[product.id] || { bg: '#EAE6DD', label: 'Standard' };
            return (
              <article
                key={product.id}
                className="group bg-surface rounded border border-border overflow-hidden flex flex-col justify-between transition-shadow hover:shadow-md"
              >
                <Link to={`/product/${product.id}`} className="block">
                  <div
                    className="h-52 flex flex-col items-center justify-center p-6 border-b border-border/60 relative"
                    style={{ backgroundColor: swatch.bg }}
                  >
                    <span className="text-[11px] font-mono uppercase tracking-wider text-stone absolute top-3 left-3">
                      {product.category}
                    </span>
                    <span className="text-2xl font-mono font-bold text-ink/20 tracking-widest">
                      0{product.id}
                    </span>
                    <span className="text-xs font-mono text-stone mt-1">{swatch.label}</span>
                  </div>

                  <div className="p-4 flex flex-col gap-1.5">
                    <div className="flex items-start justify-between gap-2">
                      <h2 className="text-sm font-semibold text-ink group-hover:text-forest transition-colors">
                        {product.name}
                      </h2>
                      <span className="text-sm font-mono font-semibold text-ink tabular-nums">
                        ${product.price.toFixed(2)}
                      </span>
                    </div>
                    <p className="text-xs text-stone leading-relaxed line-clamp-2">
                      {product.description}
                    </p>
                  </div>
                </Link>

                <div className="p-4 pt-0">
                  <button
                    type="button"
                    onClick={() => handleAdd(product)}
                    data-bugdna-action="ADD_TO_CART"
                    className="w-full py-2 px-3 rounded border border-border bg-canvas hover:bg-ink hover:text-canvas hover:border-ink text-xs font-medium flex items-center justify-center gap-1.5 transition-colors"
                  >
                    {addedId === product.id ? (
                      <>
                        <Check className="w-3.5 h-3.5" />
                        <span>Added to Bag</span>
                      </>
                    ) : (
                      <>
                        <Plus className="w-3.5 h-3.5" />
                        <span>Add to Bag</span>
                      </>
                    )}
                  </button>
                </div>
              </article>
            );
          })}
        </section>
      )}
    </div>
  );
}
