import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import styles from './LandingPage.module.css';
// Photo: Eros Reyes Cabrera / Unsplash (unsplash.com/photo-1736289162890-78f1ff4f8bd3), Unsplash License.
import heroDoctors from '../../assets/hero-doctors.jpg';

// Small inline icon set (stroke-based, à la Feather icons) so we don't pull in
// an icon library dependency just for a dozen glyphs.
const Icon = ({ path, className = styles.icon, viewBox = '0 0 24 24' }) => (
  <svg className={className} viewBox={viewBox}>
    {path}
  </svg>
);

const icons = {
  plus: <path d="M12 2v20M2 12h20" />,
  menu: <path d="M3 6h18M3 12h18M3 18h18" />,
  shield: <><path d="M12 2 3 6v6c0 5 3.8 9 9 10 5.2-1 9-5 9-10V6l-9-4z" /></>,
  folder: <path d="M3 7a2 2 0 0 1 2-2h5l2 3h7a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7z" />,
  activity: <path d="M22 12h-4l-3 9L9 3l-3 9H2" />,
  flask: <><path d="M9 3h6v4.5a4 4 0 0 0 1 2.6l4 4.4a3 3 0 0 1-2.2 5H6.2a3 3 0 0 1-2.2-5l4-4.4a4 4 0 0 0 1-2.6V3z" /><path d="M8 3h8" /></>,
  pill: <><rect x="3" y="8" width="18" height="8" rx="4" /><path d="M12 8v8" /></>,
  card: <><rect x="2" y="5" width="20" height="14" rx="2" /><path d="M2 10h20" /></>,
  lock: <><rect x="3" y="11" width="18" height="11" rx="2" /><path d="M7 11V7a5 5 0 0 1 10 0v4" /></>,
  chevronDown: <path d="M6 9l6 6 6-6" />,
  star: <path d="M12 2l3.09 6.26L22 9.27l-5 4.87L18.18 21 12 17.77 5.82 21 7 14.14l-5-4.87 6.91-1.01z" />,
  building: <path d="M3 21h18M5 21V7l7-4 7 4v14M9 9v.01M15 9v.01M9 13v.01M15 13v.01M9 17v.01M15 17v.01" />,
  heart: <path d="M22 12h-4l-3 9L9 3l-3 9H2" />,
  box: <><rect x="3" y="3" width="18" height="18" rx="2" /><path d="M9 3v18M3 9h18" /></>,
  doc: <><path d="M4 22V6a2 2 0 0 1 2-2h9l5 5v13" /><path d="M15 4v5h5" /></>,
  github: <path d="M9 19c-4.3 1.4-4.3-2.5-6-3m12 5v-3.5c0-1 .1-1.4-.5-2 2.8-.3 5.5-1.4 5.5-6a4.6 4.6 0 0 0-1.3-3.2 4.2 4.2 0 0 0-.1-3.2s-1.1-.3-3.5 1.3a12.3 12.3 0 0 0-6.2 0C6.5 2.8 5.4 3.1 5.4 3.1a4.2 4.2 0 0 0-.1 3.2A4.6 4.6 0 0 0 4 9.5c0 4.6 2.7 5.7 5.5 6-.6.6-.6 1.2-.5 2V21" />,
  twitter: <path d="M23 3a10.9 10.9 0 0 1-3.14 1.53 4.48 4.48 0 0 0-7.86 3v1A10.66 10.66 0 0 1 3 4s-4 9 5 13a11.64 11.64 0 0 1-7 2c9 5 20 0 20-11.5a4.5 4.5 0 0 0-.08-.83A7.72 7.72 0 0 0 23 3z" />,
  linkedin: <><path d="M16 8a6 6 0 0 1 6 6v7h-4v-7a2 2 0 0 0-2-2 2 2 0 0 0-2 2v7h-4v-7a6 6 0 0 1 6-6z" /><rect x="2" y="9" width="4" height="12" /><circle cx="4" cy="4" r="2" /></>,
};

const NAV_LINKS = [
  { href: '#features', label: 'Features' },
  { href: '#how-it-works', label: 'How it works' },
  { href: '#pricing', label: 'Pricing' },
  { href: '#security', label: 'Security' },
  { href: '#faq', label: 'FAQ' },
];

const FEATURES = [
  { icon: 'lock', title: 'Secure sign-in', desc: 'Role-based access with optional two-factor authentication keeps every login accountable.', tag: 'live' },
  { icon: 'folder', title: 'Patient records', desc: 'Search, update, and review patient history and visit records in seconds, not minutes.', tag: 'live' },
  { icon: 'heart', title: 'Doctor scheduling', desc: 'Manage appointments, prescriptions, and visit notes from a single doctor dashboard.', tag: 'soon' },
  { icon: 'flask', title: 'Lab workflow', desc: 'Track test requests through to results, with reports ready to share automatically.', tag: 'soon' },
  { icon: 'pill', title: 'Pharmacy & inventory', desc: 'Stay ahead of stock-outs with live inventory tracking and low-stock alerts.', tag: 'soon' },
  { icon: 'card', title: 'Billing & invoicing', desc: 'Generate invoices straight from a visit and track payments without spreadsheets.', tag: 'soon' },
];

const FAQS = [
  { q: 'Can I import our existing patient records?', a: 'Yes — you can import records from a CSV export, or start fresh and add patients as they visit. Our onboarding team can help with a bulk import on the Growth and Enterprise plans.' },
  { q: 'Is patient data hosted in our own region?', a: 'On Enterprise plans, MediCore can be deployed on your own infrastructure or a private cloud region of your choice — see the Deployment Guide in the repository for details.' },
  { q: 'What happens after the free trial?', a: 'You can keep using the Starter plan for free with up to 3 staff accounts, or upgrade to Growth at any time. No card is required to start.' },
  { q: 'Can doctors, nurses, and admins have different access levels?', a: 'Yes — every account has a role (Admin, Doctor, Nurse, Lab Technician, Pharmacist, or Billing Clerk) and only sees the data and actions relevant to that role.' },
];

function Reveal({ children, className = '' }) {
  const ref = useRef(null);
  useEffect(() => {
    const el = ref.current;
    if (!el || !('IntersectionObserver' in window)) {
      el?.classList.add(styles.revealIn);
      return;
    }
    const io = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          el.classList.add(styles.revealIn);
          io.unobserve(el);
        }
      },
      { threshold: 0.15 }
    );
    io.observe(el);
    return () => io.disconnect();
  }, []);
  return (
    <div ref={ref} className={`${styles.reveal} ${className}`}>
      {children}
    </div>
  );
}

export default function LandingPage() {
  const [openFaq, setOpenFaq] = useState(0);

  useEffect(() => {
    document.getElementById('landing-menu-toggle')?.setAttribute('aria-expanded', 'false');
  }, []);

  function toggleMobileMenu() {
    const menu = document.getElementById('landing-mobile-menu');
    const btn = document.getElementById('landing-menu-toggle');
    const isOpen = menu.classList.toggle(styles.mobileMenuOpen);
    btn.setAttribute('aria-expanded', String(isOpen));
  }

  function closeMobileMenu() {
    document.getElementById('landing-mobile-menu')?.classList.remove(styles.mobileMenuOpen);
  }

  return (
    <div className={styles.shell}>
      <div className={styles.announce}>
        🎉 MediCore just launched Patient Records &amp; Auth — <a href="#features">see what's live</a>
      </div>

      <nav className={styles.navTop}>
        <div className={styles.wrap}>
          <Link to="/" className={styles.brand}>
            <span className={styles.mark}><Icon path={icons.plus} className={`${styles.iconSm} ${styles.iconWhite}`} /></span>
            MediCore
          </Link>
          <div className={styles.navlinks}>
            {NAV_LINKS.map((l) => (
              <a key={l.href} href={l.href}>{l.label}</a>
            ))}
          </div>
          <div className={styles.navActions}>
            <Link to="/login" className={`${styles.btn} ${styles.btnGhost}`}>Log in</Link>
            <Link to="/register" className={`${styles.btn} ${styles.btnPrimary}`}>Get started</Link>
            <button
              id="landing-menu-toggle"
              className={styles.menuToggle}
              aria-label="Toggle menu"
              onClick={toggleMobileMenu}
            >
              <Icon path={icons.menu} />
            </button>
          </div>
        </div>
        <div id="landing-mobile-menu" className={styles.mobileMenu}>
          {NAV_LINKS.map((l) => (
            <a key={l.href} href={l.href} onClick={closeMobileMenu}>{l.label}</a>
          ))}
        </div>
      </nav>

      <section className={styles.hero}>
        <div className={`${styles.blob} ${styles.blob1}`}></div>
        <div className={`${styles.blob} ${styles.blob2}`}></div>
        <div className={`${styles.wrap} ${styles.heroGrid}`}>
          <div>
            <div className={styles.eyebrowPill}><span className={styles.eyebrowDot}></span>Now onboarding hospital partners</div>
            <h1 className={styles.h1}>Hospital operations, <span className={styles.hl}>made effortless.</span></h1>
            <p className={styles.lede}>MediCore brings patients, doctors, labs, pharmacy, and billing into one clean platform — so your staff spend less time on paperwork and more time on care.</p>
            <div className={styles.ctaRow}>
              <Link to="/register" className={`${styles.btn} ${styles.btnPrimary} ${styles.btnLg}`}>Get started free</Link>
              <a href="#how-it-works" className={`${styles.btn} ${styles.btnOutline} ${styles.btnLg}`}>See how it works</a>
            </div>
            <div className={styles.avatarsRow}>
              <div className={styles.avatarStack}><span></span><span></span><span></span></div>
              <p><strong>Built for teams of any size</strong> — from a single clinic to a full hospital floor</p>
            </div>
          </div>
          <div className={styles.heroVisual}>
            <div className={styles.heroPhotoFrame}>
              <img
                src={heroDoctors}
                alt="Two clinicians in white coats, part of a MediCore care team"
                className={styles.heroPhoto}
              />
              <div className={styles.heroPhotoShade}></div>
            </div>
            <div className={styles.statBadgeRail}></div>
            <div className={`${styles.statBadge} ${styles.statBadgeTop}`}>
              <div className={styles.statBadgeNum}>9</div>
              <div className={styles.statBadgeLabel}>Connected<br />modules</div>
            </div>
            <div className={`${styles.statBadge} ${styles.statBadgeBottom}`}>
              <div className={styles.statBadgeNum}>&lt;300ms</div>
              <div className={styles.statBadgeLabel}>Avg. API<br />response</div>
            </div>
            <div className={styles.heroPillBar}>
              <Link to="/register" className={`${styles.btn} ${styles.btnPrimary}`}>Get started free</Link>
              <Link to="/login" className={`${styles.btn} ${styles.btnGhost}`}>Log in</Link>
            </div>
          </div>
        </div>
      </section>

      <div className={styles.trustStrip}>
        <div className={styles.wrap}>
          <p>Trusted by clinics and hospital teams across the region</p>
          <div className={styles.trustLogos}>
            <div className={styles.trustLogo}><Icon path={icons.building} />Riverside Clinic</div>
            <div className={styles.trustLogo}><Icon path={icons.heart} />Kigali General</div>
            <div className={styles.trustLogo}><Icon path={icons.box} />Sunrise Health Group</div>
            <div className={styles.trustLogo}><Icon path={icons.doc} />Northgate Medical</div>
          </div>
        </div>
      </div>

      <div className={styles.statsStrip}>
        <div className={`${styles.wrap} ${styles.statsRow}`}>
          <div><div className={styles.statNum}>9</div><div className={styles.statLabel}>Connected modules</div></div>
          <div><div className={styles.statNum}>&lt;300ms</div><div className={styles.statLabel}>Avg. API response</div></div>
          <div><div className={styles.statNum}>99.5%</div><div className={styles.statLabel}>Uptime target</div></div>
          <div><div className={styles.statNum}>24/7</div><div className={styles.statLabel}>System monitoring</div></div>
        </div>
      </div>

      <section className={styles.section} id="features">
        <div className={styles.wrap}>
          <Reveal className={styles.sectionHead}>
            <div className={styles.kicker}>FEATURES</div>
            <h2>Everything your staff needs, in one place</h2>
            <p>From the front desk to the pharmacy counter, every team works from the same live record.</p>
          </Reveal>
          <div className={styles.features}>
            {FEATURES.map((f) => (
              <div className={styles.featureCard} key={f.title}>
                <div className={styles.featureIcon}><Icon path={icons[f.icon]} /></div>
                <h3>{f.title}</h3>
                <p>{f.desc}</p>
                <span className={`${styles.featureTag} ${f.tag === 'live' ? styles.featureTagLive : styles.featureTagSoon}`}>
                  {f.tag === 'live' ? 'Available now' : 'Coming soon'}
                </span>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className={`${styles.section} ${styles.sectionSoft}`}>
        <div className={styles.wrap}>
          <Reveal className={styles.spotlight}>
            <div className={styles.spText}>
              <div className={styles.kicker}>PATIENT RECORDS</div>
              <h3>See every patient's story at a glance</h3>
              <p>Search by name, jump straight to a visit history, and never lose a record between shifts. Everything updates in real time as your team works.</p>
              <ul className={styles.spList}>
                <li><span className={styles.tick}>✓</span>Full medical history, versioned and timestamped</li>
                <li><span className={styles.tick}>✓</span>Instant search across thousands of records</li>
                <li><span className={styles.tick}>✓</span>Attachments and scanned documents in one place</li>
              </ul>
            </div>
            <div className={styles.spVisual}>
              <div className={styles.spTable}>
                <div className={`${styles.spTRow} ${styles.spTRowHead}`}><span>Patient</span><span>Last visit</span><span>Status</span></div>
                <div className={styles.spTRow}><span className={styles.spTName}><span className={`${styles.spTDot} ${styles.spTDotOk}`}></span>Grace Uwase</span><span>Today</span><span>Active</span></div>
                <div className={styles.spTRow}><span className={styles.spTName}><span className={`${styles.spTDot} ${styles.spTDotOk}`}></span>Jean Mugisha</span><span>Yesterday</span><span>Active</span></div>
                <div className={styles.spTRow}><span className={styles.spTName}><span className={`${styles.spTDot} ${styles.spTDotWait}`}></span>Alice Keza</span><span>3 days ago</span><span>Follow-up</span></div>
                <div className={styles.spTRow}><span className={styles.spTName}><span className={`${styles.spTDot} ${styles.spTDotOk}`}></span>Eric Habimana</span><span>1 week ago</span><span>Active</span></div>
              </div>
            </div>
          </Reveal>

          <Reveal className={`${styles.spotlight} ${styles.spotlightRev}`}>
            <div className={styles.spVisual}>
              <div className={styles.spTable}>
                <div className={`${styles.spTRow} ${styles.spTRowHead}`}><span>Metric</span><span>This week</span><span>Trend</span></div>
                <div className={styles.spTRow}><span className={styles.spTName}>Avg. wait time</span><span>12 min</span><span className={styles.trendUp}>↓ 18%</span></div>
                <div className={styles.spTRow}><span className={styles.spTName}>Lab turnaround</span><span>1.4 hrs</span><span className={styles.trendUp}>↓ 9%</span></div>
                <div className={styles.spTRow}><span className={styles.spTName}>Bed utilization</span><span>76%</span><span className={styles.trendSteady}>→ steady</span></div>
              </div>
            </div>
            <div className={styles.spText}>
              <div className={styles.kicker}>ANALYTICS</div>
              <h3>Know what's happening, without asking around</h3>
              <p>Role-scoped dashboards give every team the numbers that matter to them — a doctor sees their own patient load, an admin sees the whole hospital.</p>
              <ul className={styles.spList}>
                <li><span className={styles.tick}>✓</span>Live KPIs: wait time, turnaround, revenue</li>
                <li><span className={styles.tick}>✓</span>Role-scoped views, no manual filtering</li>
                <li><span className={styles.tick}>✓</span>Export any report as PDF or CSV</li>
              </ul>
            </div>
          </Reveal>
        </div>
      </section>

      <section className={styles.section} id="how-it-works">
        <div className={styles.wrap}>
          <Reveal className={styles.sectionHead}>
            <div className={styles.kicker}>HOW IT WORKS</div>
            <h2>Up and running in three steps</h2>
            <p>No lengthy IT rollout — your team can be working in the platform the same day.</p>
          </Reveal>
          <Reveal className={styles.steps}>
            <div className={styles.step}>
              <div className={styles.stepNum}>1</div>
              <h3>Create your account</h3>
              <p>Set up your hospital's workspace and invite your staff with the right roles from day one.</p>
            </div>
            <div className={styles.step}>
              <div className={styles.stepNum}>2</div>
              <h3>Add your patients</h3>
              <p>Import existing records or start fresh — patient profiles are ready to use immediately.</p>
            </div>
            <div className={styles.step}>
              <div className={styles.stepNum}>3</div>
              <h3>Start your first visit</h3>
              <p>Doctors, nurses, and admin staff all work from the same up-to-date record.</p>
            </div>
          </Reveal>
        </div>
      </section>

      <section className={`${styles.section} ${styles.sectionSoft}`} id="pricing">
        <div className={styles.wrap}>
          <Reveal className={styles.sectionHead}>
            <div className={styles.kicker}>PRICING</div>
            <h2>Simple plans that scale with you</h2>
            <p>Start free. Upgrade when your team grows. No setup fees, cancel anytime.</p>
          </Reveal>
          <Reveal className={styles.pricingGrid}>
            <div className={styles.priceCard}>
              <div className={styles.priceName}>Starter</div>
              <div className={styles.priceDesc}>For a single clinic getting started</div>
              <div className={styles.priceAmount}>Free</div>
              <ul className={styles.priceList}>
                <li><span className={`${styles.tick} ${styles.tickSm}`}>✓</span>Up to 3 staff accounts</li>
                <li><span className={`${styles.tick} ${styles.tickSm}`}>✓</span>Patient records &amp; search</li>
                <li><span className={`${styles.tick} ${styles.tickSm}`}>✓</span>Secure sign-in with MFA</li>
                <li><span className={`${styles.tick} ${styles.tickSm}`}>✓</span>Community support</li>
              </ul>
              <Link to="/register" className={`${styles.btn} ${styles.btnOutline} ${styles.btnBlock}`}>Get started</Link>
            </div>
            <div className={`${styles.priceCard} ${styles.priceCardPopular}`}>
              <div className={styles.popularBadge}>Most popular</div>
              <div className={styles.priceName}>Growth</div>
              <div className={styles.priceDesc}>For a growing clinic or small hospital</div>
              <div className={styles.priceAmount}>$79<span>/month</span></div>
              <ul className={styles.priceList}>
                <li><span className={`${styles.tick} ${styles.tickSm}`}>✓</span>Up to 25 staff accounts</li>
                <li><span className={`${styles.tick} ${styles.tickSm}`}>✓</span>Everything in Starter</li>
                <li><span className={`${styles.tick} ${styles.tickSm}`}>✓</span>Doctor scheduling &amp; lab workflow</li>
                <li><span className={`${styles.tick} ${styles.tickSm}`}>✓</span>Priority email support</li>
              </ul>
              <Link to="/register" className={`${styles.btn} ${styles.btnPrimary} ${styles.btnBlock}`}>Get started</Link>
            </div>
            <div className={styles.priceCard}>
              <div className={styles.priceName}>Enterprise</div>
              <div className={styles.priceDesc}>For hospital networks with custom needs</div>
              <div className={styles.priceAmount}>Custom</div>
              <ul className={styles.priceList}>
                <li><span className={`${styles.tick} ${styles.tickSm}`}>✓</span>Unlimited staff accounts</li>
                <li><span className={`${styles.tick} ${styles.tickSm}`}>✓</span>Everything in Growth</li>
                <li><span className={`${styles.tick} ${styles.tickSm}`}>✓</span>Dedicated onboarding &amp; SLA</li>
                <li><span className={`${styles.tick} ${styles.tickSm}`}>✓</span>On-prem / private cloud option</li>
              </ul>
              <a href="mailto:hello@medicore.example" className={`${styles.btn} ${styles.btnOutline} ${styles.btnBlock}`}>Talk to us</a>
            </div>
          </Reveal>
        </div>
      </section>

      <section className={`${styles.section} ${styles.noPadBottom}`}>
        <div className={styles.wrap}>
          <Reveal className={styles.quoteCard}>
            <div className={styles.stars}>
              {[0, 1, 2, 3, 4].map((i) => <Icon key={i} path={icons.star} className={styles.iconFill} />)}
            </div>
            <p className={styles.quoteText}>"We replaced four separate tools with MediCore in a single week. Our front desk and lab team finally see the same information at the same time."</p>
            <div className={styles.quotePerson}>
              <div className={styles.quoteAvatar}></div>
              <div className={styles.quoteMeta}>
                <div className="name" style={{ fontWeight: 700, fontSize: '0.95rem' }}>Dr. Amara Nkusi</div>
                <div className="role" style={{ fontSize: '0.85rem', color: 'var(--muted)' }}>Head of Operations, Riverside Clinic</div>
              </div>
            </div>
          </Reveal>
        </div>
      </section>

      <section className={`${styles.section} ${styles.sectionSoft}`} id="security">
        <div className={`${styles.wrap} ${styles.securityGrid}`}>
          <Reveal>
            <div className={styles.kicker}>SECURITY</div>
            <h2>Built to keep patient data safe</h2>
            <p>Security isn't bolted on — it's part of how every feature is designed and reviewed before it ships.</p>
            <ul className={styles.checkList}>
              <li><span className={styles.tick}>✓</span>Passwords are encrypted and never stored in plain text</li>
              <li><span className={styles.tick}>✓</span>Optional two-factor authentication for every account</li>
              <li><span className={styles.tick}>✓</span>All data encrypted in transit with HTTPS</li>
              <li><span className={styles.tick}>✓</span>Full activity log of who accessed what, and when</li>
              <li><span className={styles.tick}>✓</span>Automatic lockout after repeated failed sign-in attempts</li>
            </ul>
          </Reveal>
          <Reveal className={styles.securityPanel}>
            <div className={styles.securityRow}><span className="k">Encryption in transit</span><span className="v">TLS 1.2+</span></div>
            <div className={styles.securityRow}><span className="k">Password storage</span><span className="v">Hashed (bcrypt)</span></div>
            <div className={styles.securityRow}><span className="k">Two-factor auth</span><span className="v">Supported</span></div>
            <div className={styles.securityRow}><span className="k">Access control</span><span className="v">Role-based</span></div>
            <div className={styles.securityRow}><span className="k">Audit trail</span><span className="v">Every record change</span></div>
          </Reveal>
        </div>
      </section>

      <section className={styles.section} id="faq">
        <div className={styles.wrap}>
          <Reveal className={styles.sectionHead}>
            <div className={styles.kicker}>FAQ</div>
            <h2>Questions, answered</h2>
            <p>Can't find what you're looking for? Reach out and we'll get back to you.</p>
          </Reveal>
          <Reveal className={styles.faqList}>
            {FAQS.map((item, i) => {
              const isOpen = openFaq === i;
              return (
                <div className={`${styles.faqItem} ${isOpen ? styles.faqItemOpen : ''}`} key={item.q}>
                  <button
                    type="button"
                    className={styles.faqSummary}
                    aria-expanded={isOpen}
                    aria-controls={`faq-panel-${i}`}
                    onClick={() => setOpenFaq(isOpen ? -1 : i)}
                  >
                    {item.q}
                    <Icon path={icons.chevronDown} className={`${styles.iconSm} ${styles.faqChevron}`} />
                  </button>
                  <div
                    className={styles.faqAnswer}
                    id={`faq-panel-${i}`}
                    role="region"
                  >
                    <div className={styles.faqAnswerInner}>
                      <p>{item.a}</p>
                    </div>
                  </div>
                </div>
              );
            })}
          </Reveal>
        </div>
      </section>

      <section className={`${styles.section} ${styles.noPadTop}`}>
        <div className={styles.wrap}>
          <Reveal className={styles.ctaBanner}>
            <h2>Ready to bring your hospital onto one platform?</h2>
            <p>Join the hospitals modernizing their operations with MediCore. Get set up today — no credit card required.</p>
            <div className={styles.ctaRow}>
              <Link to="/register" className={`${styles.btn} ${styles.btnPrimary} ${styles.btnLg}`}>Get started free</Link>
              <a href="mailto:hello@medicore.example" className={`${styles.btn} ${styles.btnOutline} ${styles.btnLg}`}>Talk to us</a>
            </div>
            <p className={styles.ctaFine}>Free 30-day trial on Growth · No credit card required</p>
          </Reveal>
        </div>
      </section>

      <footer className={styles.footer}>
        <div className={styles.wrap}>
          <div className={styles.footerGrid}>
            <div className={styles.footerBrand}>
              <div className={styles.brand}>
                <span className={styles.mark}><Icon path={icons.plus} className={`${styles.iconSm} ${styles.iconWhite}`} /></span>
                MediCore
              </div>
              <p>The all-in-one operations platform for modern hospitals — patients, staff, and billing, together.</p>
              <div className={styles.socialRow}>
                <a href="https://github.com/Mugabo-art/enterprise-healthcare-platform" aria-label="GitHub"><Icon path={icons.github} className={styles.iconSm} /></a>
                <a href="#" aria-label="Twitter"><Icon path={icons.twitter} className={styles.iconSm} /></a>
                <a href="#" aria-label="LinkedIn"><Icon path={icons.linkedin} className={styles.iconSm} /></a>
              </div>
            </div>
            <div className={styles.footerCol}>
              <h5>Product</h5>
              <ul>
                <li><a href="#features">Features</a></li>
                <li><a href="#pricing">Pricing</a></li>
                <li><a href="#security">Security</a></li>
                <li><a href="#how-it-works">How it works</a></li>
              </ul>
            </div>
            <div className={styles.footerCol}>
              <h5>Company</h5>
              <ul>
                <li><a href="https://github.com/Mugabo-art/enterprise-healthcare-platform">GitHub</a></li>
                <li><a href="#">About</a></li>
                <li><a href="#">Contact</a></li>
              </ul>
            </div>
            <div className={styles.footerCol}>
              <h5>Legal</h5>
              <ul>
                <li><a href="https://github.com/Mugabo-art/enterprise-healthcare-platform/blob/main/LICENSE">License</a></li>
                <li><a href="#">Privacy</a></li>
                <li><a href="#">Terms</a></li>
              </ul>
            </div>
          </div>
          <div className={styles.footerBottom}>
            <span>© 2026 MediCore. Built by Mugabo Bonheur.</span>
            <span>Enterprise Healthcare Platform</span>
          </div>
        </div>
      </footer>
    </div>
  );
}
