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
          '0%, 100%': { opacity: '1', filter: 'brightness(1)' },
          '50%': { opacity: '0.6', filter: 'brightness(1.5) drop-shadow(0 0 6px rgba(200, 241, 105, 0.6))' }
        },
        highlightFlash: {
          '0%': { backgroundColor: 'rgba(200, 241, 105, 0.15)', borderColor: 'var(--accent)' },
          '100%': { backgroundColor: 'transparent', borderColor: 'var(--line)' }
        },
        slideDown: {
          '0%': { opacity: '0', transform: 'translateY(-6px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' }
        }
      },
      animation: {
        'pulse-glow': 'pulseGlow 1.2s ease-in-out',
        'highlight-flash': 'highlightFlash 1.2s cubic-bezier(0.16, 1, 0.3, 1)',
        'slide-down': 'slideDown 200ms cubic-bezier(0.16, 1, 0.3, 1)'
      }
    },
  },
  plugins: [],
}
