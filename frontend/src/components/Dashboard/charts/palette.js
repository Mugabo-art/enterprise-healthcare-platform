// Categorical order validated with the dataviz skill's validator against this
// app's white card surface (#FFFFFF): passes lightness/chroma/CVD-separation/
// normal-vision gates as an adjacent (bar) pairlist. Two slots (aqua, yellow)
// sit below 3:1 contrast on white — the "relief rule" applies, so every chart
// using this palette ships visible direct value labels, never color alone.
export const CATEGORICAL = ['#2a78d6', '#eb6834', '#1baf7a', '#eda100'];

export function categoricalColor(index) {
  return CATEGORICAL[index % CATEGORICAL.length];
}

// Brand hue for the single-series visits trend — a sequential/brand encoding,
// not a categorical one, so it doesn't need CVD-adjacent validation. Matches
// the dashboard shell's blue brand override (see AppShell.module.css .shell),
// same values as LandingPage's --green/--green-dark.
export const TREND_LINE = '#1B4FCC';
export const TREND_DOT = '#2F6FED';
export const TREND_FILL = 'rgba(47, 111, 237, 0.10)';
