import styles from './BreakdownBarChart.module.css';

export default function BreakdownBarChart({ title, categories }) {
  const max = Math.max(1, ...categories.map((c) => c.value));

  return (
    <div>
      <div className={styles.chart} role="img" aria-label={title}>
        {categories.map((c) => {
          const heightPct = (c.value / max) * 100;
          return (
            <div key={c.label} className={styles.column}>
              <div className={styles.barTrack}>
                <span className={styles.capLabel}>{c.value}</span>
                <button
                  type="button"
                  className={styles.bar}
                  style={{ height: `${Math.max(heightPct, 2)}%`, background: c.color }}
                  aria-label={`${c.label}: ${c.value}`}
                />
              </div>
              <span className={styles.categoryLabel}>{c.label}</span>
            </div>
          );
        })}
      </div>

      <details className={styles.tableToggle}>
        <summary>View as table</summary>
        <table>
          <thead>
            <tr><th>Category</th><th>Count</th></tr>
          </thead>
          <tbody>
            {categories.map((c) => (
              <tr key={c.label}><td>{c.label}</td><td>{c.value}</td></tr>
            ))}
          </tbody>
        </table>
      </details>
    </div>
  );
}
