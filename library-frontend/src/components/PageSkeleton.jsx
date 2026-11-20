import { Skeleton, Stack } from '@mui/material'

/** Placeholder while a lazily loaded page downloads (see AppRoutes). */
export default function PageSkeleton() {
  return (
    <Stack spacing={2} aria-busy="true" aria-label="Loading">
      <Skeleton width={240} height={48} />
      <Skeleton variant="rounded" height={160} />
      <Skeleton variant="rounded" height={160} />
    </Stack>
  )
}
