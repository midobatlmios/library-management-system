import { Alert, Pagination, Skeleton, Stack } from '@mui/material'

/**
 * Shows a paged backend list: skeletons on first load, an error, an empty
 * state, the items (faded while the next page loads) and the pagination.
 *
 * page is 1-based here; the backend's pageNumber is 0-based.
 */
export default function PagedList({ data, error, loading, page, onPageChange, empty, renderItem, skeletonHeight = 96 }) {
  return (
    <Stack spacing={2}>
      {error && <Alert severity="error">{error}</Alert>}

      {!data && loading ? (
        <Stack spacing={2}>
          {[0, 1, 2].map((i) => (
            <Skeleton key={i} variant="rounded" height={skeletonHeight} />
          ))}
        </Stack>
      ) : data?.empty ? (
        empty
      ) : (
        data && (
          <Stack spacing={2} sx={{ opacity: loading ? 0.5 : 1, transition: 'opacity 150ms' }}>
            {data.content.map(renderItem)}
          </Stack>
        )
      )}

      {data && data.totalPages > 1 && (
        <Pagination
          count={data.totalPages}
          page={page}
          onChange={(_, p) => onPageChange(p)}
          color="primary"
          sx={{ alignSelf: 'center' }}
        />
      )}
    </Stack>
  )
}
