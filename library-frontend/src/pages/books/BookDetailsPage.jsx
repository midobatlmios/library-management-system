import { useCallback, useEffect, useState } from 'react'
import { Alert, Breadcrumbs, Button, Card, CardContent, Chip, Link, Skeleton, Stack, Typography } from '@mui/material'
import { Link as RouterLink, useParams } from 'react-router-dom'
import { getBook } from '../../api/books.js'
import { getBookRatingStats } from '../../api/reviews.js'
import AvailabilityChip from '../../components/books/AvailabilityChip.jsx'
import BookActionButton from '../../components/books/BookActionButton.jsx'
import BookCover from '../../components/books/BookCover.jsx'
import RatingSummary from '../../components/books/RatingSummary.jsx'
import ReviewList from '../../components/books/ReviewList.jsx'
import WishlistButton from '../../components/books/WishlistButton.jsx'
import WriteReview from '../../components/reviews/WriteReview.jsx'
import { formatPublished } from '../../utils/format.js'
import { useAsync } from '../../utils/useAsync.js'

function Detail({ label, value }) {
  if (value === null || value === undefined || value === '') return null
  return (
    <div className="flex gap-3 py-1.5">
      <span className="sign-caps w-28 shrink-0 text-[11px] leading-6" style={{ color: 'var(--mui-palette-text-secondary)' }}>
        {label}
      </span>
      <Typography variant="body2" sx={{ fontWeight: 600 }} className="tabular">
        {value}
      </Typography>
    </div>
  )
}

export default function BookDetailsPage() {
  const { id } = useParams()
  const [book, setBook] = useState(null)
  const [error, setError] = useState(null)

  const loadBook = useCallback(() => {
    return getBook(id)
      .then((b) => {
        setBook(b)
        setError(null)
      })
      .catch((e) => setError({ message: e.message, status: e.status }))
  }, [id])

  useEffect(() => {
    loadBook()
  }, [loadBook])

  const loadStats = useCallback(() => getBookRatingStats(id), [id])
  const stats = useAsync(loadStats)

  // Changing this key remounts the review list, so it reloads from page 1.
  const [reviewsKey, setReviewsKey] = useState(0)
  const handleReviewCreated = () => {
    stats.reload()
    setReviewsKey((k) => k + 1)
  }

  if (error) {
    return (
      <Stack spacing={2} sx={{ alignItems: 'flex-start' }}>
        <Alert severity={error.status === 404 ? 'info' : 'error'}>
          {error.status === 404 ? 'This book does not exist or is no longer in the catalog.' : error.message}
        </Alert>
        <Button component={RouterLink} to="/books" variant="contained">
          Back to the catalog
        </Button>
      </Stack>
    )
  }

  if (!book) {
    return (
      <div className="flex flex-col gap-6 md:flex-row">
        <Skeleton variant="rounded" className="w-full md:w-64 aspect-[2/3] !h-auto" />
        <Stack spacing={1} className="flex-1">
          <Skeleton width="60%" height={48} />
          <Skeleton width="40%" />
          <Skeleton variant="rounded" height={120} />
        </Stack>
      </div>
    )
  }

  return (
    <Stack spacing={4}>
      <Breadcrumbs>
        <Link component={RouterLink} to="/books" underline="hover" color="inherit">
          Catalog
        </Link>
        {book.genreName && (
          <Link component={RouterLink} to={`/books?genre=${book.genreId}`} underline="hover" color="inherit">
            {book.genreName}
          </Link>
        )}
        <Typography color="text.primary" className="line-clamp-1">
          {book.title}
        </Typography>
      </Breadcrumbs>

      <div className="flex flex-col gap-8 md:flex-row">
        <div className="w-48 self-center md:w-64 md:self-start shrink-0">
          <BookCover book={book} />
        </div>

        <Stack spacing={2} className="flex-1 min-w-0">
          <div className="flex items-start gap-2">
            <div className="flex-1">
              <Typography variant="h4" component="h1" sx={{ fontWeight: 700 }}>
                {book.title}
              </Typography>
              <Typography variant="h6" component="p" color="text.secondary">
                {book.author}
              </Typography>
            </div>
            <WishlistButton bookId={book.id} />
          </div>

          <div className="flex flex-wrap gap-2">
            <AvailabilityChip book={book} size="medium" />
            {book.genreName && <Chip label={book.genreName} variant="outlined" />}
          </div>

          <BookActionButton book={book} onChanged={loadBook} />

          {book.description && <Typography sx={{ whiteSpace: 'pre-line' }}>{book.description}</Typography>}

          {/* The book's details on a white enamel plate. */}
          <div className="plaque-white max-w-xl px-7 py-5">
            <Detail label="ISBN" value={book.isbn} />
            <Detail label="Publisher" value={book.publisher} />
            <Detail label="Published" value={formatPublished(book.publicationDate)} />
            <Detail label="Language" value={book.language} />
            <Detail label="Pages" value={book.pages} />
            <Detail label="Copies" value={`${book.availableCopies} on the shelf of ${book.totalCopies}`} />
          </div>
        </Stack>
      </div>

      <Card variant="outlined">
        <CardContent>
          <Stack spacing={3}>
            <Typography variant="h5" component="h2" sx={{ fontWeight: 700 }}>
              Reviews
            </Typography>
            {stats.data ? <RatingSummary stats={stats.data} /> : <Skeleton variant="rounded" height={100} />}
            <WriteReview key={`write-${reviewsKey}`} book={book} onCreated={handleReviewCreated} />
            {stats.data?.totalReviews > 0 && (
              <ReviewList key={reviewsKey} bookId={book.id} bookTitle={book.title} onChanged={stats.reload} />
            )}
          </Stack>
        </CardContent>
      </Card>
    </Stack>
  )
}
