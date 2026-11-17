/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        bg: 'var(--bg)',
        panel: {
          DEFAULT: 'var(--panel)',
          hover: 'var(--panel-hover)',
          elevated: 'var(--panel-elevated)'
        },
        line: {
          DEFAULT: 'var(--line)',
          subtle: 'var(--line-subtle)'
        },
        content: {
          DEFAULT: 'var(--text)',
          muted: 'var(--muted)'
        },
        accent: {
          DEFAULT: 'var(--accent)',
          dim: 'var(--accent-dim)'
        },
        danger: {
          DEFAULT: 'var(--danger)',
          dim: 'var(--danger-dim)'
        },
        warn: {
          DEFAULT: 'var(--warn)',
          dim: 'var(--warn-dim)'
        }
      },
      fontFamily: {
        sans: ['Geist', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'sans-serif'],
        mono: ['"Geist Mono"', 'ui-monospace', 'SFMono-Regular', 'Menlo', 'Monaco', 'Consolas', 'monospace']
      },
      keyframes: {
        pulseGlow: {
          '0%': { opacity: '1', transform: 'scaleY(1)' },
          '40%': { opacity: '0.55', transform: 'scaleY(1.14)' },
          '100%': { opacity: '1', transform: 'scaleY(1)' }
        },
        highlightFlash: {
          '0%': { opacity: '0.55', transform: 'translateX(3px)' },
          '100%': { opacity: '1', transform: 'translateX(0)' }
        },
        slideDown: {
          '0%': { opacity: '0', transform: 'translateY(-6px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' }
        }
      },
      animation: {
        'pulse-glow': 'pulseGlow 360ms cubic-bezier(0.16, 1, 0.3, 1)',
        'highlight-flash': 'highlightFlash 1.2s cubic-bezier(0.16, 1, 0.3, 1)',
        'slide-down': 'slideDown 220ms cubic-bezier(0.16, 1, 0.3, 1)'
      }
    },
  },
  plugins: [],
}
