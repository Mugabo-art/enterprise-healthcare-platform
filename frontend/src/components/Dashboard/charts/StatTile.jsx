import { Icon } from '../../Layout/icons.jsx';
import styles from './StatTile.module.css';

function formatCompact(value) {
  if (value >= 1_000_000) return `${(value / 1_000_000).toFixed(1)}M`;
  if (value >= 1_000) return `${(value / 1_000).toFixed(1)}K`;
  return String(value);
}

export default function StatTile({ label, value, hint, icon, badge, breakdown }) {
  const hasBreakdown = breakdown && breakdown.length > 0;

  return (
    <div className={`${styles.tile} ${hasBreakdown ? styles.tileRich : ''}`}>
      <div className={styles.tileMain}>
        <div className={styles.tileTop}>
          {icon && (
            <span className={styles.iconBadge}>
              <Icon path={icon} className={styles.iconBadgeSvg} />
            </span>
          )}
          <span className={styles.kebab} aria-hidden="true">
            <span /><span /><span />
          </span>
        </div>
        <span className={styles.label}>{label}</span>
        <span className={styles.value}>{formatCompact(value)}</span>
        {hint && <span className={styles.hint}>{hint}</span>}
        {badge && <span className={styles.badge}>{badge}</span>}
      </div>

      {hasBreakdown && (
        <ul className={styles.breakdown}>
          {breakdown.map((b) => (
            <li key={b.label} className={styles.breakdownRow}>
              <span className={styles.dot} style={{ background: b.color }} />
              <span className={styles.breakdownLabel}>{b.label}</span>
              <span className={styles.breakdownValue}>{b.value}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
