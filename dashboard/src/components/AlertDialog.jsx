import React, { useEffect } from 'react';
import { AlertTriangle } from 'lucide-react';

export default function AlertDialog({
  isOpen,
  title = 'Are you sure?',
  description = 'This action cannot be undone.',
  confirmText = 'Delete',
  cancelText = 'Cancel',
  onConfirm,
  onCancel,
  isDestructive = true,
}) {
  useEffect(() => {
    if (!isOpen) return;
    const handleKey = (e) => {
      if (e.key === 'Escape') onCancel?.();
    };
    window.addEventListener('keydown', handleKey);
    return () => window.removeEventListener('keydown', handleKey);
  }, [isOpen, onCancel]);

  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-slide-down overscroll-contain"
      role="alertdialog"
      aria-modal="true"
    >
      <div className="w-full max-w-md rounded bg-panel border border-line shadow-2xl p-5 flex flex-col gap-4">
        <div className="flex items-start gap-3">
          <div className="p-2 rounded bg-danger/10 border border-danger/30 text-danger flex-shrink-0">
            <AlertTriangle className="w-5 h-5" />
          </div>
          <div className="flex flex-col gap-1">
            <h3 className="font-mono text-sm font-bold text-content">{title}</h3>
            <p className="text-xs text-content-muted leading-relaxed">{description}</p>
          </div>
        </div>

        <div className="flex items-center justify-end gap-2 pt-2 border-t border-line">
          <button
            type="button"
            onClick={onCancel}
            className="px-3 py-1.5 rounded text-xs font-mono text-content-muted hover:text-content hover:bg-line/40 transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-accent"
          >
            {cancelText}
          </button>
          <button
            type="button"
            onClick={onConfirm}
            className={`px-3 py-1.5 rounded text-xs font-mono font-semibold transition-colors focus-visible:outline-none focus-visible:ring-2 ${
              isDestructive
                ? 'bg-danger text-white hover:bg-danger/90 focus-visible:ring-danger'
                : 'bg-accent text-bg hover:bg-accent/90 focus-visible:ring-accent'
            }`}
          >
            {confirmText}
          </button>
        </div>
      </div>
    </div>
  );
}

