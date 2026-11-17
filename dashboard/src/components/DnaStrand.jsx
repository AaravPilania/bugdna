import React, { useMemo } from 'react';

/**
 * 64-Cell DNA Strand Component
 * Maps a SHA-256 hex string (64 characters) to 4 nucleotide visual tones:
 *   - 0..3 : #C8F169 (Signal Lime)
 *   - 4..7 : #4E82EE (Electric Cobalt)
 *   - 8..B : #F2B544 (Amber)
 *   - C..F : #8B9097 (Slate)
 * If isRegression is true, mutation points are accented with #FF5D5D (Danger).
 */
export default function DnaStrand({
  fingerprint = '',
  isRegression = false,
  isPulsing = false,
  size = 'md', // 'sm' (strip for table row) or 'lg' (header banner)
  showLabels = false,
  className = '',
}) {
  const cells = useMemo(() => {
    const hex = (fingerprint || '').padEnd(64, '0').slice(0, 64).toLowerCase();
    const result = [];
    for (let i = 0; i < 64; i++) {
      const char = hex[i];
      const val = parseInt(char, 16);
      let tone = 'slate';
      let color = '#8B9097';

      if (val >= 0 && val <= 3) {
        tone = 'lime';
        color = '#C8F169';
      } else if (val >= 4 && val <= 7) {
        tone = 'cobalt';
        color = '#6495ED';
      } else if (val >= 8 && val <= 11) {
        tone = 'amber';
        color = '#F2B544';
      }

      // If regression, mark specific mutation points
      const isMutationTick = isRegression && (i === 12 || i === 28 || i === 44 || i === 60);
      if (isMutationTick) {
        tone = 'mutation';
        color = '#FF5D5D';
      }

      result.push({ char, val, tone, color, isMutationTick, index: i });
    }
    return result;
  }, [fingerprint, isRegression]);

  if (size === 'sm') {
    return (
      <div
        className={`flex items-center gap-[1px] h-2.5 overflow-hidden rounded-[2px] bg-bg/80 border border-line/60 ${
          isPulsing ? 'animate-pulse-glow ring-1 ring-accent' : ''
        } ${className}`}
        title={`DNA Fingerprint: ${fingerprint}`}
        aria-label={`DNA strand for fingerprint ${fingerprint}`}
      >
        {cells.map((cell) => (
          <div
            key={cell.index}
            className="flex-1 h-full transition-colors duration-150"
            style={{
              backgroundColor: cell.color,
              opacity: cell.isMutationTick ? 1 : 0.85,
            }}
          />
        ))}
      </div>
    );
  }

  return (
    <div className={`flex flex-col gap-1.5 ${className}`}>
      <div
        className={`grid grid-cols-32 sm:grid-cols-64 gap-[2px] p-2 bg-panel rounded border ${
          isRegression ? 'border-danger/40' : 'border-line'
        } ${isPulsing ? 'animate-pulse-glow ring-1 ring-accent' : ''}`}
        aria-label={`Full 64-character DNA strand: ${fingerprint}`}
      >
        {cells.map((cell) => (
          <div
            key={cell.index}
            className={`group relative h-6 rounded-[2px] flex items-center justify-center transition-all ${
              cell.isMutationTick ? 'ring-1 ring-danger' : ''
            }`}
            style={{ backgroundColor: `${cell.color}25` }}
          >
            <div
              className="w-1.5 h-3 rounded-[1px] transition-transform group-hover:scale-125"
              style={{ backgroundColor: cell.color }}
            />
            {/* Tooltip on cell hover */}
            <div className="absolute -top-7 hidden group-hover:flex z-20 px-1 py-0.5 rounded bg-bg text-[10px] font-mono text-content border border-line whitespace-nowrap shadow-md pointer-events-none">
              #{cell.index}: {cell.char}
            </div>
          </div>
        ))}
      </div>
      {showLabels && (
        <div className="flex items-center justify-between text-[11px] font-mono uppercase tracking-wider text-content-muted">
          <span>5′ GENOME INDEX 0</span>
          {isRegression && (
            <span className="text-danger flex items-center gap-1 font-semibold">
              <span className="w-1.5 h-1.5 rounded-full bg-danger animate-pulse" />
              MUTATION DETECTED
            </span>
          )}
          <span>INDEX 63 3′</span>
        </div>
      )}
    </div>
  );
}

