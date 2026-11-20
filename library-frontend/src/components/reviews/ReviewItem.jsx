import { Chip, Rating, Stack, Typography } from '@mui/material'
import VerifiedIcon from '@mui/icons-material/Verified'
import { formatDate } from '../../utils/format.js'

/**
 * One review. `heading` replaces the reviewer's name (My reviews shows the
 * book title there), `badge` is an extra chip, `actions` are buttons at the bottom.
 */
export default function ReviewItem({ review, heading, badge, actions }) {
  return (
    <Stack spacing={0.75}>
      <div className="flex flex-wrap items-center gap-2">
        <Rating value={review.rating} readOnly size="small" />
        {review.title && <Typography sx={{ fontWeight: 600 }}>{review.title}</Typography>}
        {badge}
      </div>
      <Typography variant="body2" color="text.secondary" component="div" className="flex flex-wrap items-center gap-x-2">
        {heading ?? <span>{review.userFullName}</span>}
        <span>·</span>
        <span>
          {formatDate(review.createdAt)}
          {review.updatedAt && review.updatedAt.slice(0, 16) !== review.createdAt?.slice(0, 16) && ' (edited)'}
        </span>
        {review.isVerifiedRead && (
          <Chip icon={<VerifiedIcon />} label="Borrowed this book" size="small" color="success" variant="outlined" />
        )}
      </Typography>
      <Typography sx={{ whiteSpace: 'pre-line' }}>{review.reviewText}</Typography>
      <div className="flex flex-wrap items-center gap-2">
        {actions}
        {review.helpfulCount > 0 && (
          <Typography variant="caption" color="text.secondary">
            {review.helpfulCount} found this helpful
          </Typography>
        )}
      </div>
    </Stack>
  )
}
