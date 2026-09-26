import { useEffect, useRef, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext.jsx';
import { Icon, icons } from './icons.jsx';
import styles from './AppShell.module.css';

const THEME_KEY = 'medicore_theme';

function initialTheme() {
  try {
    return localStorage.getItem(THEME_KEY) || 'light';
  } catch {
    return 'light';
  }
}

function initials(email) {
  if (!email) return '?';
  const name = email.split('@')[0];
  return name.slice(0, 2).toUpperCase();
}

export default function AppShell({ children }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [theme, setTheme] = useState(initialTheme);
  const [query, setQuery] = useState('');
  const searchInputRef = useRef(null);

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    try {
      localStorage.setItem(THEME_KEY, theme);
    } catch {
      // ignore — theme just won't persist across reloads
    }
  }, [theme]);

  // ⌘K / Ctrl+K jumps straight to the search box, matching the shortcut hint
  // rendered in the search pill below.
  useEffect(() => {
    function handleKeyDown(e) {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault();
        searchInputRef.current?.focus();
      }
    }
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  const isPatientRole = user?.role === 'PATIENT';
  const onPatientDetail = location.pathname.startsWith('/dashboard/patients');
  const onDashboard = location.pathname === '/dashboard' && !onPatientDetail;

  async function handleLogout() {
    await logout();
    navigate('/');
  }

  function handleSearchSubmit(e) {
    e.preventDefault();
    if (!query.trim()) return;
    navigate(`/dashboard?q=${encodeURIComponent(query.trim())}`);
  }

  const navItems = [
    { label: 'Dashboard', icon: icons.grid, to: '/dashboard', active: onDashboard || (isPatientRole && location.pathname === '/dashboard') },
  ];
  if (!isPatientRole) {
    navItems.push({ label: 'Patients', icon: icons.users, to: '/dashboard#patients-list', active: onPatientDetail });
  }
  if (user?.role === 'DOCTOR') {
    navItems.push({ label: 'Schedule', icon: icons.calendar, to: '/dashboard/schedule', active: location.pathname === '/dashboard/schedule' });
  }
  if (user?.role === 'PHARMACIST' || user?.role === 'ADMIN') {
    navItems.push({ label: 'Pharmacy', icon: icons.pill, to: '/dashboard/pharmacy', active: location.pathname === '/dashboard/pharmacy' });
  }

  return (
    <div className={styles.shell}>
      <aside className={styles.sidebar}>
        <Link to="/dashboard" className={styles.brand}>
          <span className={styles.mark}>
            <svg viewBox="0 0 24 24"><path d="M12 2v20M2 12h20" /></svg>
          </span>
          MediCore
        </Link>

        <nav className={styles.nav}>
          {navItems.map((item) => (
            <Link key={item.label} to={item.to} className={`${styles.navItem} ${item.active ? styles.navItemActive : ''}`}>
              <Icon path={item.icon} className={styles.navIcon} />
              <span className={styles.navLabel}>{item.label}</span>
            </Link>
          ))}
        </nav>
      </aside>

      <div className={styles.main}>
        <header className={styles.topbar}>
          <form className={styles.searchForm} onSubmit={handleSearchSubmit}>
            <Icon path={icons.search} className={styles.searchIcon} />
            <input
              ref={searchInputRef}
              className={styles.searchInput}
              placeholder="Search patients by last name…"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
            />
            <kbd className={styles.kbd}>⌘K</kbd>
          </form>

          <div className={styles.topbarRight}>
            <button
              type="button"
              className={styles.iconBtn}
              onClick={() => setTheme((t) => (t === 'dark' ? 'light' : 'dark'))}
              aria-label="Toggle theme"
              title={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
            >
              <Icon path={theme === 'dark' ? icons.sun : icons.moon} className={styles.icon} />
            </button>

            {user && (
              <div className={styles.userMenu}>
                <span className={styles.avatar}>{initials(user.email)}</span>
                <span className={styles.userBadge}>
                  <span className={styles.userEmail}>{user.email}</span>
                  <span className={styles.userRole}>{user.role}</span>
                </span>
                <button type="button" className={styles.iconBtn} onClick={handleLogout} aria-label="Log out" title="Log out">
                  <Icon path={icons.logout} className={styles.icon} />
                </button>
              </div>
            )}
          </div>
        </header>

        <main className={styles.content}>{children}</main>
      </div>
    </div>
  );
}
