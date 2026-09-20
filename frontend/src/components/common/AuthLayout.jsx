import { Link } from 'react-router-dom';
import styles from './AuthLayout.module.css';

const POINTS = [
  'Patient records in one place',
  'Role-based access for every team',
  'Built for teams of any size',
];

export default function AuthLayout({ children }) {
  return (
    <div className={styles.shell}>
      <div className={styles.brandPanel}>
        <div className={`${styles.blob} ${styles.blob1}`}></div>
        <div className={`${styles.blob} ${styles.blob2}`}></div>

        <Link to="/" className={styles.brand}>
          <span className={styles.mark}>
            <svg viewBox="0 0 24 24"><path d="M12 2v20M2 12h20" /></svg>
          </span>
          MediCore
        </Link>

        <div className={styles.pitch}>
          <h1>Hospital operations, made effortless.</h1>
          <ul className={styles.points}>
            {POINTS.map((p) => (
              <li key={p}>
                <span className={styles.tick}>✓</span>
                {p}
              </li>
            ))}
          </ul>
        </div>

        <p className={styles.footNote}>Trusted by clinics and hospital teams across the region.</p>
      </div>

      <div className={styles.formPanel}>
        <div className={styles.formCard}>
          <Link to="/" className={styles.backLink}>← Back to home</Link>
          {children}
        </div>
      </div>
    </div>
  );
}
