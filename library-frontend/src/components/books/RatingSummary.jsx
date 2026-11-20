import { LinearProgress, Rating, Stack, Typography } from '@mui/material'

/** Average stars, number of reviews and a bar per star level (5 down to 1). */
export default function RatingSummary({ stats }) {
  const total = stats.totalReviews || 0
  const average = Number(stats.averageRating || 0)

  if (total === 0) {
    return <Typography color="text.secondary">No reviews yet.</Typography>
  }

  return (
    <div className="flex flex-col gap-6 sm:flex-row sm:items-center">
      <Stack sx={{ alignItems: 'center' }} className="sm:w-40 shrink-0">
        <Typography variant="h3" component="p" sx={{ fontWeight: 700 }}>
          {average.toFixed(1)}
        </Typography>
        <Rating value={average} precision={0.1} readOnly />
        <Typography variant="body2" color="text.secondary">
          {total} review{total === 1 ? '' : 's'}
        </Typography>
        {stats.verifiedReaderReviews > 0 && (
          <Typography variant="caption" color="text.secondary">
            {stats.verifiedReaderReviews} from verified readers
          </Typography>
        )}
      </Stack>

      <Stack spacing={0.75} className="flex-1">
        {[5, 4, 3, 2, 1].map((stars) => {
          const count = stats.ratingDistribution?.[String(stars)] ?? 0
          return (
            <div key={stars} className="flex items-center gap-2">
              <Typography variant="body2" className="w-12 shrink-0">
                {stars} star{stars === 1 ? '' : 's'}
              </Typography>
              <LinearProgress
                variant="determinate"
                value={(count / total) * 100}
                className="flex-1"
                sx={{ height: 8, borderRadius: 4 }}
                aria-label={`${stars} stars: ${count}`}
              />
              <Typography variant="body2" color="text.secondary" className="w-8 text-right">
                {count}
              </Typography>
            </div>
          )
        })}
      </Stack>
    </div>
  )
}
