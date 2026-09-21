import styles from './StatTile.module.css';

function formatCompact(value) {
  if (value >= 1_000_000) return `${(value / 1_000_000).toFixed(1)}M`;
  if (value >= 1_000) return `${(value / 1_000).toFixed(1)}K`;
  return String(value);
}

export default function StatTile({ label, value, hint }) {
  return (
    <div className={styles.tile}>
      <span className={styles.label}>{label}</span>
      <span className={styles.value}>{formatCompact(value)}</span>
      {hint && <span className={styles.hint}>{hint}</span>}
    </div>
  );
}
