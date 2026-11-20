import { Card, CardActionArea, CardContent, Skeleton, Typography } from '@mui/material'
import { Link as RouterLink } from 'react-router-dom'

/**
 * A stat tile: label, value, and optional small lines under it. Links to `to` when given.
 * The value uses proportional figures (tabular digits look loose at this size).
 */
export default function StatCard({ label, value, lines = [], to, loading }) {
  const content = (
    <CardContent>
      <div className="sign-caps text-[11px]" style={{ color: 'var(--mui-palette-text-secondary)' }}>
        {label}
      </div>
      {loading ? (
        <Skeleton width={80} height={44} />
      ) : (
        <Typography variant="h4" component="p" sx={{ fontWeight: 800, mt: 0.5 }}>
          {value ?? '—'}
        </Typography>
      )}
      {!loading &&
        lines.filter(Boolean).map((l) => (
          <Typography key={l} variant="caption" color="text.secondary" component="p">
            {l}
          </Typography>
        ))}
    </CardContent>
  )
  return (
    <Card className="h-full">
      {to ? (
        <CardActionArea component={RouterLink} to={to} className="h-full">
          {content}
        </CardActionArea>
      ) : (
        content
      )}
    </Card>
  )
}
