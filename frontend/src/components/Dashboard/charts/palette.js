// Categorical order validated with the dataviz skill's validator against this
// app's white card surface (#FFFFFF): passes lightness/chroma/CVD-separation/
// normal-vision gates as an adjacent (bar) pairlist. Two slots (aqua, yellow)
// sit below 3:1 contrast on white — the "relief rule" applies, so every chart
// using this palette ships visible direct value labels, never color alone.
export const CATEGORICAL = ['#2a78d6', '#eb6834', '#1baf7a', '#eda100'];

export function categoricalColor(index) {
  return CATEGORICAL[index % CATEGORICAL.length];
}

// Brand hue (this app's own green ramp, from src/index.css) for the single-series
// visits trend — a sequential/brand encoding, not a categorical one, so it doesn't
// need CVD-adjacent validation.
export const TREND_LINE = '#0F7A38';
export const TREND_DOT = '#16A34A';
export const TREND_FILL = 'rgba(22, 163, 74, 0.10)';
