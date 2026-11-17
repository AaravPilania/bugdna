import React, { useEffect, useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { storeApi } from '../api';
import { useCart } from '../CartContext';
import { ArrowLeft, ShoppingBag, AlertCircle } from 'lucide-react';

export default function Product() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { addItem } = useCart();

  const [product, setProduct] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    storeApi
      .getProduct(id)
      .then((data) => setProduct(data))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [id]);

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 py-10 flex flex-col gap-6">
      <Link
        to="/"
        className="inline-flex items-center gap-1.5 text-xs font-mono text-stone hover:text-ink transition-colors w-fit"
      >
        <ArrowLeft className="w-3.5 h-3.5" />
        <span>Back to Catalog</span>
      </Link>

      {loading ? (
        <div className="h-80 rounded bg-border/40 animate-pulse" />
      ) : error || !product ? (
        <div className="p-10 rounded bg-surface border border-border text-center flex flex-col items-center gap-3">
          <div className="w-10 h-10 rounded-full bg-rust/10 text-rust flex items-center justify-center">
            <AlertCircle className="w-5 h-5" />
          </div>
          <h1 className="text-lg font-bold text-ink">Item Currently Unavailable</h1>
          <p className="text-sm text-stone max-w-md">
            Sorry, we couldn&apos;t locate product #{id}. It may have sold out or been archived.
          </p>
          <Link
            to="/"
            className="mt-2 px-4 py-2 rounded bg-ink text-canvas text-xs font-medium hover:bg-ink/90 transition-colors"
          >
            Browse Available Goods
          </Link>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-8 bg-surface p-6 sm:p-8 rounded border border-border">
          <div className="h-72 md:h-auto rounded bg-canvas border border-border/60 flex flex-col items-center justify-center p-8">
            <span className="text-xs font-mono uppercase tracking-widest text-stone">
              {product.category}
            </span>
            <span className="text-5xl font-mono font-bold text-ink/15 my-4">
              0{product.id}
            </span>
            <span className="text-xs font-mono text-stone">SKU-KOVA-00{product.id}</span>
          </div>

          <div className="flex flex-col justify-between gap-6">
            <div className="flex flex-col gap-3">
              <h1 className="text-2xl font-bold text-ink tracking-tight">{product.name}</h1>
              <p className="text-xl font-mono font-semibold text-forest tabular-nums">
                ${product.price.toFixed(2)}
              </p>
              <p className="text-sm text-stone leading-relaxed">{product.description}</p>
              <div className="pt-3 border-t border-border text-xs font-mono text-stone space-y-1">
                <p>Free ground shipping on orders over $75</p>
                <p>30-day unconditional returns</p>
              </div>
            </div>

            <div className="flex flex-col sm:flex-row gap-3">
              <button
                type="button"
                data-bugdna-action="ADD_TO_CART"
                onClick={() => {
                  addItem(product);
                  navigate('/cart');
                }}
                className="flex-1 py-2.5 px-4 rounded bg-forest text-white text-sm font-medium flex items-center justify-center gap-2 hover:bg-forest/90 transition-colors"
              >
                <ShoppingBag className="w-4 h-4" />
                <span>Add to Bag & View Cart</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

