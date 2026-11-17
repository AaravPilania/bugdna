import React, { useState } from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { CartProvider } from './CartContext';
import StoreHeader from './components/StoreHeader';
import DemoFaultsPanel from './components/DemoFaultsPanel';
import Home from './pages/Home';
import Product from './pages/Product';
import Cart from './pages/Cart';
import Checkout from './pages/Checkout';

export default function App() {
  const [faultsOpen, setFaultsOpen] = useState(false);

  return (
    <BrowserRouter>
      <CartProvider>
        <div className="min-h-dvh flex flex-col bg-canvas text-ink">
          <StoreHeader
            faultsOpen={faultsOpen}
            onToggleFaults={() => setFaultsOpen((prev) => !prev)}
          />
          <DemoFaultsPanel isOpen={faultsOpen} onClose={() => setFaultsOpen(false)} />

          <main className="flex-1">
            <Routes>
              <Route path="/" element={<Home />} />
              <Route path="/product/:id" element={<Product />} />
              <Route path="/cart" element={<Cart />} />
              <Route path="/checkout" element={<Checkout />} />
            </Routes>
          </main>

          <footer className="border-t border-border py-6 px-4 text-center text-xs font-mono text-stone">
            Kova Goods · Monitored by BugDNA Telemetry
          </footer>
        </div>
      </CartProvider>
    </BrowserRouter>
  );
}
