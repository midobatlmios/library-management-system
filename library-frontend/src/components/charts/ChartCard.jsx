import { useState } from 'react'
import { Card, CardContent, ToggleButton, ToggleButtonGroup, Typography } from '@mui/material'

/**
 * The frame every chart sits in: title, one-line subtitle, and the Chart / Table
 * switch (the table is the accessible twin of the chart: every value, no hovering).
 * `loading` keeps the previous render at reduced opacity instead of flashing.
 */
export default function ChartCard({ title, subtitle, table, loading, children }) {
  const [view, setView] = useState('chart')
  return (
    <Card component="figure" sx={{ m: 0 }}>
      <CardContent className="flex flex-col gap-3">
        <div className="flex flex-wrap items-start justify-between gap-2">
          <div>
            <Typography component="figcaption" variant="h6" sx={{ fontWeight: 800, lineHeight: 1.2 }}>
              {title}
            </Typography>
            {subtitle && (
              <Typography variant="body2" color="text.secondary">
                {subtitle}
              </Typography>
            )}
          </div>
          {table && (
            <ToggleButtonGroup size="small" exclusive value={view} onChange={(_, v) => v && setView(v)} aria-label={`${title}: view`}>
              <ToggleButton value="chart">Chart</ToggleButton>
              <ToggleButton value="table">Table</ToggleButton>
            </ToggleButtonGroup>
          )}
        </div>
        <div style={{ opacity: loading ? 0.5 : 1, transition: 'opacity 150ms' }}>
          {view === 'table' && table ? table : children}
        </div>
      </CardContent>
    </Card>
  )
}

/** A plain data table for the Table view. columns: [{ key, label, align, format }] */
export function ChartTable({ columns, rows }) {
  return (
    <div className="max-h-80 overflow-auto">
      <table className="w-full border-collapse text-[14px] tabular">
        <thead>
          <tr>
            {columns.map((c) => (
              <th
                key={c.key}
                scope="col"
                className="sticky top-0 px-2 py-1.5 font-bold"
                style={{ textAlign: c.align || 'left', background: 'var(--chart-surface)', borderBottom: '1px solid var(--chart-grid)' }}
              >
                {c.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((r, i) => (
            <tr key={i}>
              {columns.map((c) => (
                <td key={c.key} className="px-2 py-1" style={{ textAlign: c.align || 'left', borderBottom: '1px solid var(--chart-grid)' }}>
                  {c.format ? c.format(r[c.key], r) : r[c.key]}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
