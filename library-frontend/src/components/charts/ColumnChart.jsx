import { useState } from 'react'
import { axisLabel, compact, niceScale, shortDate, useWidth } from './useWidth.js'

const M = { top: 22, right: 12, bottom: 30, left: 48 }

/**
 * One column per day for a single series (no legend: the title names it).
 * Columns are at most 24px wide with a 4px rounded top and a square base.
 * Only the highest day carries a value label; every column shows its value on
 * hover or keyboard focus, and the table view lists them all.
 */
export default function ColumnChart({ rows, valueKey, color = 'var(--series-1)', height = 230, format = compact, label }) {
  const [ref, width] = useWidth()
  const [hover, setHover] = useState(null)

  const plotW = width - M.left - M.right
  const plotH = height - M.top - M.bottom
  const n = rows.length
  const values = rows.map((r) => Number(r[valueKey]) || 0)
  const max = Math.max(0, ...values)
  const { top, ticks } = niceScale(max)
  const slot = plotW / Math.max(1, n)
  const barW = Math.max(2, Math.min(24, slot - 2)) // a 2px gap between neighbours at least
  const y = (v) => M.top + plotH - (v / top) * plotH
  const peak = max > 0 ? values.indexOf(max) : -1

  const every = Math.max(1, Math.ceil(n / 5))
  const xTicks = rows.map((_, i) => i).filter((i) => (n - 1 - i) % every === 0)

  // A column path: square at the baseline, 4px rounded top corners.
  const column = (cx, v) => {
    const h = plotH - (y(v) - M.top)
    if (h <= 0) return ''
    const r = Math.min(4, barW / 2, h)
    const x0 = cx - barW / 2
    const x1 = cx + barW / 2
    const yt = y(v)
    const yb = M.top + plotH
    return `M${x0},${yb}V${yt + r}Q${x0},${yt} ${x0 + r},${yt}H${x1 - r}Q${x1},${yt} ${x1},${yt + r}V${yb}Z`
  }

  const h = hover != null ? rows[hover] : null
  const cx = (i) => M.left + slot * i + slot / 2
  const tipLeft = hover != null ? Math.min(Math.max(cx(hover) + 10, 8), width - 150) : 0

  return (
    <div ref={ref} className="relative w-full select-none">
      <svg width={width} height={height} role="img" aria-label={label} className="block">
        {ticks.map((t) => (
          <g key={t}>
            <line x1={M.left} x2={M.left + plotW} y1={y(t)} y2={y(t)} stroke="var(--chart-grid)" strokeWidth="1" />
            <text x={M.left - 8} y={y(t)} dy="0.32em" textAnchor="end" fontSize="11" fill="var(--chart-text)" className="tabular">
              {axisLabel(t, top)}
            </text>
          </g>
        ))}
        {xTicks.map((i) => (
          <text key={i} x={cx(i)} y={height - 8} textAnchor="middle" fontSize="11" fill="var(--chart-text)">
            {shortDate(rows[i].date)}
          </text>
        ))}
        {rows.map((r, i) => (
          <g key={r.date}>
            <path d={column(cx(i), values[i])} fill={color} opacity={hover == null || hover === i ? 1 : 0.55} />
            {/* the hit area is the whole slot, taller and wider than the column */}
            <rect
              x={M.left + slot * i}
              y={M.top}
              width={slot}
              height={plotH}
              fill="transparent"
              tabIndex={0}
              aria-label={`${shortDate(r.date)}: ${format(values[i])}`}
              onPointerEnter={() => setHover(i)}
              onPointerLeave={() => setHover(null)}
              onFocus={() => setHover(i)}
              onBlur={() => setHover(null)}
            />
          </g>
        ))}
        {/* the one direct label: the highest day */}
        {peak >= 0 && hover == null && (
          // Anchored so the label never runs past the chart's edges.
          <text
            x={cx(peak) > width - 70 ? cx(peak) + barW / 2 : cx(peak) < M.left + 40 ? cx(peak) - barW / 2 : cx(peak)}
            textAnchor={cx(peak) > width - 70 ? 'end' : cx(peak) < M.left + 40 ? 'start' : 'middle'}
            y={y(max) - 6}
            fontSize="12"
            fontWeight="700"
            fill="var(--mui-palette-text-primary)"
          >
            {format(max)}
          </text>
        )}
      </svg>
      {h && (
        <div
          className="pointer-events-none absolute z-10 rounded-[10px] px-3 py-2 text-[13px] shadow-lg"
          style={{ left: tipLeft, top: 4, background: 'var(--mui-palette-background-paper)', border: '1px solid var(--chart-grid)' }}
          role="status"
        >
          <div className="font-semibold" style={{ color: 'var(--chart-text)' }}>
            {shortDate(h.date)}
          </div>
          <strong className="tabular" style={{ color: 'var(--mui-palette-text-primary)' }}>
            {format(values[hover])}
          </strong>
        </div>
      )}
    </div>
  )
}
