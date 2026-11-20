import { Typography } from '@mui/material'
import { compact } from './useWidth.js'

/**
 * Ranked horizontal bars, largest first, one hue. Each row: the name, a bar
 * (14px, 4px rounded end, square at its base) and the value at the bar's tip.
 * Every value is printed, so no tooltip is needed to read it.
 */
export default function BarList({ items, color = 'var(--series-1)', format = compact, empty = 'No data yet.', max: maxRows = 8 }) {
  const sorted = [...items].sort((a, b) => b.value - a.value)
  const shown = sorted.slice(0, maxRows)
  const rest = sorted.slice(maxRows)
  // Past the limit, the tail folds into one "Other" row rather than more bars.
  if (rest.length) shown.push({ name: `Other (${rest.length})`, value: rest.reduce((s, r) => s + r.value, 0), other: true })
  const top = Math.max(1, ...shown.map((r) => r.value))

  if (!items.length) {
    return (
      <Typography variant="body2" color="text.secondary">
        {empty}
      </Typography>
    )
  }

  return (
    <ul className="m-0 flex list-none flex-col gap-2.5 p-0">
      {shown.map((r, i) => (
        <li key={`${r.name}-${i}`} className="grid grid-cols-[minmax(90px,38%)_1fr] items-center gap-3">
          <span className="truncate text-[14px] font-semibold" title={r.name}>
            {r.name}
          </span>
          <span className="flex items-center gap-2">
            <span
              className="block h-[14px]"
              style={{
                width: `${Math.max(r.value > 0 ? 2 : 0, (r.value / top) * 85)}%`,
                background: r.other ? 'var(--chart-text)' : color,
                borderRadius: '0 4px 4px 0',
              }}
            />
            <span className="text-[13px] font-bold tabular" style={{ color: 'var(--mui-palette-text-primary)' }}>
              {format(r.value)}
            </span>
          </span>
        </li>
      ))}
    </ul>
  )
}
