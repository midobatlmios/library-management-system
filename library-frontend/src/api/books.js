import client from './client.js'

// One file per backend module. Each function returns the response body.

export const getBookStats = () => client.get('/api/books/stats').then((r) => r.data)

/**
 * Paged search. params: { search, genreId, availableOnly, page, size, sortBy, sortDirection }
 * Empty values are left out so the backend uses its defaults.
 */
export const searchBooks = (params) => {
  const clean = Object.fromEntries(
    Object.entries(params).filter(([, v]) => v !== undefined && v !== null && v !== ''),
  )
  return client.get('/api/books', { params: clean }).then((r) => r.data)
}

export const getBook = (id) => client.get(`/api/books/${id}`).then((r) => r.data)
