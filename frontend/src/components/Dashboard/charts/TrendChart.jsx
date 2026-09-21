import { useMemo, useRef, useState } from 'react';
import { TREND_DOT, TREND_FILL, TREND_LINE } from './palette.js';
import styles from './TrendChart.module.css';

const WIDTH = 600;
const HEIGHT = 180;
const PAD_TOP = 24;
const PAD_BOTTOM = 24;

function formatDate(iso) {
  return new Date(`${iso}T00:00:00Z`).toLocaleDateString(undefined, { month: 'short', day: 'numeric', timeZone: 'UTC' });
}

export default function TrendChart({ data, title }) {
  const svgRef = useRef(null);
  const [hoverIndex, setHoverIndex] = useState(null);

  const { points, path, areaPath } = useMemo(() => {
    const max = Math.max(1, ...data.map((d) => d.count));
    const plotHeight = HEIGHT - PAD_TOP - PAD_BOTTOM;
    const xStep = data.length > 1 ? WIDTH / (data.length - 1) : 0;
    const pts = data.map((d, i) => ({
      x: data.length > 1 ? i * xStep : WIDTH / 2,
      y: PAD_TOP + plotHeight - (d.count / max) * plotHeight,
      date: d.date,
      count: d.count,
    }));
    const linePath = pts.map((p, i) => `${i === 0 ? 'M' : 'L'}${p.x},${p.y}`).join(' ');
    const area = pts.length
      ? `${linePath} L${pts[pts.length - 1].x},${HEIGHT - PAD_BOTTOM} L${pts[0].x},${HEIGHT - PAD_BOTTOM} Z`
      : '';
    return { points: pts, path: linePath, areaPath: area };
  }, [data]);

  function handlePointerMove(e) {
    const svg = svgRef.current;
    if (!svg || points.length === 0) return;
    const rect = svg.getBoundingClientRect();
    const relX = ((e.clientX - rect.left) / rect.width) * WIDTH;
    let nearest = 0;
    let nearestDist = Infinity;
    points.forEach((p, i) => {
      const dist = Math.abs(p.x - relX);
      if (dist < nearestDist) {
        nearestDist = dist;
        nearest = i;
      }
    });
    setHoverIndex(nearest);
  }

  const hovered = hoverIndex != null ? points[hoverIndex] : null;
  const last = points[points.length - 1];

  return (
    <div className={styles.wrap}>
      <svg
        ref={svgRef}
        className={styles.svg}
        viewBox={`0 0 ${WIDTH} ${HEIGHT}`}
        preserveAspectRatio="none"
        role="img"
        aria-label={title}
        onPointerMove={handlePointerMove}
        onPointerLeave={() => setHoverIndex(null)}
      >
        <line x1={0} y1={HEIGHT - PAD_BOTTOM} x2={WIDTH} y2={HEIGHT - PAD_BOTTOM} className={styles.baseline} />

        {areaPath && <path d={areaPath} fill={TREND_FILL} stroke="none" />}
        {path && <path d={path} fill="none" stroke={TREND_LINE} strokeWidth={2} strokeLinejoin="round" strokeLinecap="round" />}

        {hovered && (
          <line x1={hovered.x} y1={PAD_TOP} x2={hovered.x} y2={HEIGHT - PAD_BOTTOM} className={styles.crosshair} />
        )}

        {points.map((p, i) => (
          <circle
            key={p.date}
            cx={p.x}
            cy={p.y}
            r={i === hoverIndex || i === points.length - 1 ? 5 : 3}
            fill={TREND_DOT}
            stroke="var(--white)"
            strokeWidth={2}
          />
        ))}

        {last && (
          <text x={last.x} y={last.y - 12} textAnchor="end" className={styles.endLabel}>
            {last.count}
          </text>
        )}

        {points[0] && (
          <text x={points[0].x} y={HEIGHT - 6} textAnchor="start" className={styles.axisLabel}>
            {formatDate(points[0].date)}
          </text>
        )}
        {last && (
          <text x={last.x} y={HEIGHT - 6} textAnchor="end" className={styles.axisLabel}>
            {formatDate(last.date)}
          </text>
        )}
      </svg>

      {hovered && (
        <div
          className={styles.tooltip}
          style={{ left: `${(hovered.x / WIDTH) * 100}%`, top: `${(hovered.y / HEIGHT) * 100}%` }}
        >
          <strong>{hovered.count}</strong> visit{hovered.count === 1 ? '' : 's'}
          <span>{formatDate(hovered.date)}</span>
        </div>
      )}

      <details className={styles.tableToggle}>
        <summary>View as table</summary>
        <table>
          <thead>
            <tr><th>Date</th><th>Visits</th></tr>
          </thead>
          <tbody>
            {data.map((d) => (
              <tr key={d.date}><td>{formatDate(d.date)}</td><td>{d.count}</td></tr>
            ))}
          </tbody>
        </table>
      </details>
    </div>
  );
}
