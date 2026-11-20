import { useEffect, useMemo, useRef, useState } from 'react'
import {
  Alert,
  Box,
  Button,
  Card,
  Drawer,
  FormControl,
  FormControlLabel,
  InputAdornment,
  InputLabel,
  MenuItem,
  Pagination,
  Select,
  Skeleton,
  Stack,
  Switch,
  TextField,
  Typography,
} from '@mui/material'
import SearchIcon from '@mui/icons-material/Search'
import CategoryIcon from '@mui/icons-material/Category'
import { useSearchParams } from 'react-router-dom'
import { searchBooks } from '../../api/books.js'
import { getGenreHierarchy } from '../../api/genres.js'
import BookCard from '../../components/books/BookCard.jsx'
import GenreTree from '../../components/books/GenreTree.jsx'

const PAGE_SIZE = 12

// Sort choices shown to the user -> the backend's sortBy + sortDirection.
const SORTS = {
  newest: { label: 'Newest in the library', sortBy: 'createdAt', sortDirection: 'DESC' },
  title: { label: 'Title (A–Z)', sortBy: 'title', sortDirection: 'ASC' },
  author: { label: 'Author (A–Z)', sortBy: 'author', sortDirection: 'ASC' },
  published: { label: 'Recently published', sortBy: 'publicationDate', sortDirection: 'DESC' },
  available: { label: 'Most copies available', sortBy: 'availableCopies', sortDirection: 'DESC' },
}

/** Finds a genre anywhere in the tree (for the "Showing: Fiction" label). */
function findGenre(genres, id) {
  for (const g of genres) {
    if (g.id === id) return g
    const found = findGenre(g.subGenres || [], id)
    if (found) return found
  }
  return null
}

/**
 * The catalog. All filters live in the URL (?q=&genre=&available=1&sort=&page=),
 * so the Back button, a page reload, or a shared link keep the same results.
 */
export default function CatalogPage() {
  const [params, setParams] = useSearchParams()
  const q = params.get('q') || ''
  const genreId = params.get('genre') ? Number(params.get('genre')) : null
  const availableOnly = params.get('available') === '1'
  const sortKey = SORTS[params.get('sort')] ? params.get('sort') : 'newest'
  const page = Math.max(1, Number(params.get('page')) || 1) // 1-based in the URL, 0-based for the backend

  /**
   * Changes some URL params. Any filter change goes back to page 1.
   * Uses the function form of setParams so it always starts from the
   * latest URL (the search box calls this 400 ms later).
   */
  const updateParams = (changes, { keepPage = false } = {}) => {
    setParams((prev) => {
      const next = new URLSearchParams(prev)
      Object.entries(changes).forEach(([k, v]) => {
        if (v === null || v === '' || v === false) next.delete(k)
        else next.set(k, String(v))
      })
      if (!keepPage) next.delete('page')
      return next
    })
  }

  // ---- search box: typed text is applied 400 ms after the user stops typing
  const [searchText, setSearchText] = useState(q)
  const searchTimer = useRef(null)
  useEffect(() => () => clearTimeout(searchTimer.current), []) // stop the timer when leaving the page
  const handleSearchChange = (value) => {
    setSearchText(value)
    clearTimeout(searchTimer.current)
    searchTimer.current = setTimeout(() => updateParams({ q: value.trim() }), 400)
  }

  // ---- genres (loaded once)
  const [genres, setGenres] = useState([])
  const [genresError, setGenresError] = useState(null)
  const [genreDrawerOpen, setGenreDrawerOpen] = useState(false)
  useEffect(() => {
    getGenreHierarchy()
      .then(setGenres)
      .catch((e) => setGenresError(e.message))
  }, [])

  // ---- books (reloaded whenever a filter in the URL changes)
  // Each answer remembers which filters it was for (key). While the newest
  // answer has not arrived yet, we are loading and keep showing the old books.
  const requestKey = JSON.stringify([q, genreId, availableOnly, sortKey, page])
  const [response, setResponse] = useState(null) // { key, data } or { key, error }
  const result = response?.data ?? null
  const error = response?.key === requestKey ? response.error : null
  const loading = response?.key !== requestKey
  useEffect(() => {
    let cancelled = false
    const sort = SORTS[sortKey]
    searchBooks({
      search: q,
      genreId,
      availableOnly: availableOnly || undefined,
      page: page - 1,
      size: PAGE_SIZE,
      sortBy: sort.sortBy,
      sortDirection: sort.sortDirection,
    })
      .then((data) => !cancelled && setResponse({ key: requestKey, data }))
      // On error keep the previous books on screen and show the message above them.
      .catch((e) => !cancelled && setResponse((prev) => ({ key: requestKey, data: prev?.data, error: e.message })))
    return () => {
      cancelled = true
    }
  }, [q, genreId, availableOnly, sortKey, page, requestKey])

  const selectedGenre = useMemo(() => (genreId ? findGenre(genres, genreId) : null), [genres, genreId])

  const selectGenre = (id) => {
    updateParams({ genre: id })
    setGenreDrawerOpen(false)
  }

  const clearFilters = () => {
    clearTimeout(searchTimer.current)
    setSearchText('')
    setParams(new URLSearchParams())
  }

  const genrePanel = genresError ? (
    <Alert severity="error">{genresError}</Alert>
  ) : (
    <GenreTree genres={genres} selectedId={genreId} onSelect={selectGenre} />
  )

  return (
    <Stack spacing={3}>
      <div className="flex flex-wrap items-center gap-x-5 gap-y-2">
        <h1 className="plaque plaque-sm rivets sign-caps m-0 inline-block px-9 py-2.5 text-xl">Catalog</h1>
        <Typography color="text.secondary" sx={{ fontWeight: 600 }}>
          {selectedGenre ? `Genre: ${selectedGenre.name}` : 'All genres'}
          {result && ` · ${result.totalElements} book${result.totalElements === 1 ? '' : 's'}`}
        </Typography>
      </div>

      <div className="flex gap-6">
        {/* Sidebar on wide screens */}
        <Card variant="outlined" className="hidden md:block w-60 shrink-0 self-start p-2">
          <Typography variant="overline" color="text.secondary" sx={{ px: 1.5 }}>
            Genres
          </Typography>
          {genrePanel}
        </Card>

        {/* Drawer on phones */}
        <Drawer open={genreDrawerOpen} onClose={() => setGenreDrawerOpen(false)}>
          <Box className="w-72 p-2">
            <Typography variant="overline" color="text.secondary" sx={{ px: 1.5 }}>
              Genres
            </Typography>
            {genrePanel}
          </Box>
        </Drawer>

        <Stack spacing={2} className="flex-1 min-w-0">
          {/* Filters bar */}
          <div className="flex flex-col gap-2 sm:flex-row sm:flex-wrap sm:items-center">
            <TextField
              size="small"
              placeholder="Search title, author or ISBN"
              value={searchText}
              onChange={(e) => handleSearchChange(e.target.value)}
              className="sm:flex-1 sm:min-w-56"
              slotProps={{
                input: {
                  startAdornment: (
                    <InputAdornment position="start">
                      <SearchIcon fontSize="small" />
                    </InputAdornment>
                  ),
                },
                htmlInput: { 'aria-label': 'Search books' },
              }}
            />
            <FormControl size="small" className="sm:w-56">
              <InputLabel id="sort-label">Sort by</InputLabel>
              <Select
                labelId="sort-label"
                label="Sort by"
                value={sortKey}
                onChange={(e) => updateParams({ sort: e.target.value === 'newest' ? null : e.target.value })}
              >
                {Object.entries(SORTS).map(([key, s]) => (
                  <MenuItem key={key} value={key}>
                    {s.label}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <div className="flex items-center justify-between gap-2">
              <FormControlLabel
                control={
                  <Switch
                    checked={availableOnly}
                    onChange={(e) => updateParams({ available: e.target.checked ? '1' : null })}
                  />
                }
                label="Available only"
              />
              <Button
                variant="outlined"
                size="small"
                startIcon={<CategoryIcon />}
                onClick={() => setGenreDrawerOpen(true)}
                className="md:hidden"
              >
                Genres
              </Button>
            </div>
          </div>

          {error && <Alert severity="error">{error}</Alert>}

          {/* Results */}
          {loading && !result ? (
            <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5">
              {Array.from({ length: 8 }).map((_, i) => (
                <Skeleton key={i} variant="rounded" className="aspect-[2/4] !h-auto" />
              ))}
            </div>
          ) : result && result.empty ? (
            <Card variant="outlined" className="p-8 text-center">
              <Typography variant="h6" component="p" gutterBottom>
                No books found
              </Typography>
              <Typography color="text.secondary" sx={{ mb: 2 }}>
                Try another search, another genre, or turn off "Available only".
              </Typography>
              <Button variant="contained" onClick={clearFilters}>
                Clear all filters
              </Button>
            </Card>
          ) : (
            result && (
              <div
                className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 transition-opacity"
                style={{ opacity: loading ? 0.5 : 1 }}
              >
                {result.content.map((book) => (
                  <BookCard key={book.id} book={book} />
                ))}
              </div>
            )
          )}

          {result && result.totalPages > 1 && (
            <Pagination
              count={result.totalPages}
              page={page}
              onChange={(_, p) => {
                updateParams({ page: p === 1 ? null : p }, { keepPage: true })
                window.scrollTo({ top: 0, behavior: 'smooth' })
              }}
              color="primary"
              sx={{ alignSelf: 'center' }}
            />
          )}
        </Stack>
      </div>
    </Stack>
  )
}
