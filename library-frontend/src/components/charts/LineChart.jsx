import { useState } from 'react'
import { axisLabel, compact, niceScale, shortDate, useWidth } from './useWidth.js'

const M = { top: 14, right: 92, bottom: 30, left: 40 }

/**
 * Lines over days. rows: [{ date: 'YYYY-MM-DD', ...values }];
 * series: [{ key, label, color }] (color = a CSS var such as 'var(--series-1)').
 * - 2px lines, hairline solid grid, a legend for 2+ series, end labels only
 *   when they don't collide.
 * - A crosshair snaps to the nearest day and one tooltip lists every series;
 *   arrow keys move it when the chart has keyboard focus.
 */
export default function LineChart({ rows, series, height = 230, format = compact, label }) {
  const [ref, width] = useWidth()
  const [hover, setHover] = useState(null) // index of the day under the pointer

  const plotW = width - M.left - M.right
  const plotH = height - M.top - M.bottom
  const n = rows.length
  const max = Math.max(0, ...rows.flatMap((r) => series.map((s) => Number(r[s.key]) || 0)))
  const { top, ticks } = niceScale(max)
  const x = (i) => M.left + (n <= 1 ? plotW / 2 : (i / (n - 1)) * plotW)
  const y = (v) => M.top + plotH - (v / top) * plotH

  const path = (key) => rows.map((r, i) => `${i ? 'L' : 'M'}${x(i).toFixed(1)},${y(Number(r[key]) || 0).toFixed(1)}`).join('')

  // About five date ticks, always including the last day.
  const every = Math.max(1, Math.ceil(n / 5))
  const xTicks = rows.map((_, i) => i).filter((i) => (n - 1 - i) % every === 0)

  // End labels: drawn only if they sit at least 16px apart.
  const ends = series.map((s) => ({ ...s, value: Number(rows[n - 1]?.[s.key]) || 0, y: y(Number(rows[n - 1]?.[s.key]) || 0) }))
  const endsFit = ends.every((a, i) => ends.every((b, j) => i === j || Math.abs(a.y - b.y) >= 16))

  const pick = (clientX, rect) => {
    const px = clientX - rect.left
    const i = Math.round(((px - M.left) / plotW) * (n - 1))
    setHover(Math.max(0, Math.min(n - 1, i)))
  }

  const onKey = (e) => {
    if (e.key === 'ArrowRight') setHover((h) => Math.min(n - 1, (h ?? n - 1) + 1))
    else if (e.key === 'ArrowLeft') setHover((h) => Math.max(0, (h ?? n - 1) - 1))
    else if (e.key === 'Escape') setHover(null)
    else return
    e.preventDefault()
  }

  const h = hover != null ? rows[hover] : null
  const tipLeft = hover != null ? Math.min(Math.max(x(hover) + 12, 8), width - 170) : 0

  return (
    <div ref={ref} className="relative w-full select-none">
      {series.length > 1 && (
        <ul className="m-0 mb-2 flex list-none flex-wrap gap-4 p-0 text-[13px] font-semibold" style={{ color: 'var(--chart-text)' }}>
          {series.map((s) => (
            <li key={s.key} className="flex items-center gap-1.5">
              <svg width="18" height="8" aria-hidden="true">
                <line x1="1" y1="4" x2="17" y2="4" stroke={s.color} strokeWidth="2.5" strokeLinecap="round" />
              </svg>
              {s.label}
            </li>
          ))}
        </ul>
      )}
      <svg
        width={width}
        height={height}
        role="img"
        aria-label={label}
        tabIndex={0}
        onKeyDown={onKey}
        onFocus={() => setHover((h) => h ?? n - 1)}
        onBlur={() => setHover(null)}
        className="block outline-offset-4"
      >
        {/* grid + y ticks */}
        {ticks.map((t) => (
          <g key={t}>
            <line x1={M.left} x2={M.left + plotW} y1={y(t)} y2={y(t)} stroke="var(--chart-grid)" strokeWidth="1" />
            <text x={M.left - 8} y={y(t)} dy="0.32em" textAnchor="end" fontSize="11" fill="var(--chart-text)" className="tabular">
              {axisLabel(t, top)}
            </text>
          </g>
        ))}
        {/* x ticks */}
        {xTicks.map((i) => (
          <text key={i} x={x(i)} y={height - 8} textAnchor="middle" fontSize="11" fill="var(--chart-text)">
            {shortDate(rows[i].date)}
          </text>
        ))}
        {/* lines */}
        {series.map((s) => (
          <path key={s.key} d={path(s.key)} fill="none" stroke={s.color} strokeWidth="2" strokeLinejoin="round" strokeLinecap="round" />
        ))}
        {/* end labels: value in text ink, a dot in the series colour */}
        {endsFit &&
          ends.map((s) => (
            <g key={s.key}>
              <circle cx={x(n - 1)} cy={s.y} r="4" fill={s.color} stroke="var(--chart-surface)" strokeWidth="2" />
              <text x={x(n - 1) + 9} y={s.y} dy="0.32em" fontSize="12" fontWeight="700" fill="var(--mui-palette-text-primary)">
                {format(s.value)}
                <tspan fontWeight="500" fill="var(--chart-text)"> {s.label.toLowerCase()}</tspan>
              </text>
            </g>
          ))}
        {/* crosshair */}
        {hover != null && (
          <g pointerEvents="none">
            <line x1={x(hover)} x2={x(hover)} y1={M.top} y2={M.top + plotH} stroke="var(--chart-text)" strokeWidth="1" />
            {series.map((s) => (
              <circle key={s.key} cx={x(hover)} cy={y(Number(h[s.key]) || 0)} r="4.5" fill={s.color} stroke="var(--chart-surface)" strokeWidth="2" />
            ))}
          </g>
        )}
        {/* the whole plot is the hit area: the crosshair finds the day */}
        <rect
          x={M.left}
          y={M.top}
          width={plotW}
          height={plotH}
          fill="transparent"
          onPointerMove={(e) => pick(e.clientX, e.currentTarget.ownerSVGElement.getBoundingClientRect())}
          onPointerLeave={() => setHover(null)}
        />
      </svg>
      {h && (
        <div
          className="pointer-events-none absolute z-10 min-w-36 rounded-[10px] px-3 py-2 text-[13px] shadow-lg"
          style={{ left: tipLeft, top: series.length > 1 ? 36 : 4, background: 'var(--mui-palette-background-paper)', border: '1px solid var(--chart-grid)' }}
          role="status"
        >
          <div className="mb-1 font-semibold" style={{ color: 'var(--chart-text)' }}>
            {shortDate(h.date)}
          </div>
          {series.map((s) => (
            <div key={s.key} className="flex items-center gap-2">
              <svg width="14" height="6" aria-hidden="true">
                <line x1="1" y1="3" x2="13" y2="3" stroke={s.color} strokeWidth="2.5" strokeLinecap="round" />
              </svg>
              <strong className="tabular" style={{ color: 'var(--mui-palette-text-primary)' }}>
                {format(Number(h[s.key]) || 0)}
              </strong>
              <span style={{ color: 'var(--chart-text)' }}>{s.label}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
