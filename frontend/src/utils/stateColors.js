// Default colors per workflow state category — matches the previous fixed
// .category-* CSS colors, now used as the fallback when a state has no
// custom color of its own.
export const CATEGORY_DEFAULT_COLORS = {
  to_do: '#9ca3af',
  in_progress: '#f59e0b',
  done: '#22c55e'
};

const FALLBACK_COLOR = '#9ca3af';

export function resolveStateColor(state) {
  return state?.color || CATEGORY_DEFAULT_COLORS[state?.category] || FALLBACK_COLOR;
}

const HEX_COLOR_RE = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i;

// Picks black or white text for readable contrast against a given hex fill.
export function pickReadableTextColor(hexColor) {
  const match = HEX_COLOR_RE.exec(hexColor || '');
  if (!match) return '#1a1d23';
  const [r, g, b] = [match[1], match[2], match[3]].map((h) => parseInt(h, 16));
  const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
  return luminance > 0.6 ? '#1a1d23' : '#ffffff';
}
