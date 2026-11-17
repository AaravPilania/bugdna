import React, { createContext, useContext, useState } from 'react';

const CartContext = createContext(null);

export function CartProvider({ children }) {
  const [items, setItems] = useState([
    {
      id: 1,
      name: 'Minimalist Canvas Sneaker',
      category: 'Footwear',
      price: 85.0,
      qty: 1,
    },
  ]);
  const [coupon, setCoupon] = useState(null); // { code: 'SAVE20', discountPercent: 20 }

  const addItem = (product) => {
    setItems((prev) => {
      const existing = prev.find((i) => i.id === product.id);
      if (existing) {
        return prev.map((i) => (i.id === product.id ? { ...i, qty: i.qty + 1 } : i));
      }
      return [...prev, { ...product, qty: 1 }];
    });
  };

  const removeItem = (id) => {
    setItems((prev) => prev.filter((i) => i.id !== id));
  };

  const updateQty = (id, delta) => {
    setItems((prev) =>
      prev
        .map((i) => (i.id === id ? { ...i, qty: Math.max(0, i.qty + delta) } : i))
        .filter((i) => i.qty > 0)
    );
  };

  const clearCart = () => {
    setItems([]);
    setCoupon(null);
  };

  const subtotal = items.reduce((sum, i) => sum + i.price * i.qty, 0);
  const discount = coupon ? subtotal * (coupon.discountPercent / 100) : 0;
  const total = Math.max(0, subtotal - discount);

  return (
    <CartContext.Provider
      value={{
        items,
        coupon,
        setCoupon,
        addItem,
        removeItem,
        updateQty,
        clearCart,
        subtotal,
        discount,
        total,
        totalCount: items.reduce((s, i) => s + i.qty, 0),
      }}
    >
      {children}
    </CartContext.Provider>
  );
}

export function useCart() {
  return useContext(CartContext);
}

