import client from './client.js'

/**
 * Everything the admin pages call. All of these need ROLE_ADMIN on the backend.
 * Money sent to the backend is in centimes (minor units): 25.50 MAD -> 2550.
 */

const get = (url, params) => client.get(url, { params }).then((r) => r.data)
const post = (url, body, params) => client.post(url, body, { params }).then((r) => r.data)
const put = (url, body) => client.put(url, body).then((r) => r.data)
const del = (url) => client.delete(url).then((r) => r.data)

/** Drops empty values so the backend uses its defaults. */
const clean = (params = {}) =>
  Object.fromEntries(Object.entries(params).filter(([, v]) => v !== undefined && v !== null && v !== ''))

// ---- dashboard statistics
export const getBookStats = () => get('/api/books/stats')
export const getLoanStats = () => get('/api/book-loans/statistics')
export const getFinesCollected = () => get('/api/fines/statistics/collected')
export const getFinesOutstanding = () => get('/api/fines/statistics/outstanding')
export const getPaymentStats = () => get('/api/payments/admin/statistics')
export const getPlanStats = () => get('/api/subscription-plans/statistics')
export const getUserStats = () => get('/api/users/statistics')
export const getReviewStats = () => get('/api/reviews/admin/statistics')
/** { from, to, currency, days: [{ date, checkouts, returns, collected }] } for the last `days` days. */
export const getDailyActivity = (days) => get('/api/admin/statistics/daily', { days })

// ---- books (includeInactive lists soft-deleted books too)
export const searchBooksAdmin = (params) => get('/api/books', clean({ includeInactive: true, ...params }))
export const createBook = (book) => post('/api/books', book)
export const createBooksBulk = (books) => post('/api/books/bulk', books)
export const updateBook = (id, book) => put(`/api/books/${id}`, book)
export const softDeleteBook = (id) => del(`/api/books/${id}`)
export const hardDeleteBook = (id) => del(`/api/books/${id}/permanent`)

// ---- genres (an admin's GET /api/genres includes inactive ones)
export const getAllGenres = () => get('/api/genres')
export const createGenre = (genre) => post('/api/genres', genre)
export const createGenresBulk = (genres) => post('/api/genres/bulk', genres)
export const updateGenre = (id, genre) => put(`/api/genres/${id}`, genre)
export const softDeleteGenre = (id) => del(`/api/genres/${id}`)
export const hardDeleteGenre = (id) => del(`/api/genres/${id}/hard`)

// ---- plans (an admin's list includes inactive ones unless `active` is set)
export const getAllPlans = () => get('/api/subscription-plans', { size: 100, sortBy: 'price' }).then((p) => p.content)
export const createPlan = (plan) => post('/api/subscription-plans', plan)
export const updatePlan = (id, plan) => put(`/api/subscription-plans/${id}`, plan)
export const deactivatePlan = (id) => del(`/api/subscription-plans/${id}`)

// ---- users
export const getAllUsers = () => get('/api/users/list')
export const getUser = (id) => get(`/api/users/${id}`)
export const getUserLoans = (id, params) => get(`/api/book-loans/user/${id}`, clean(params))
export const getUserSubscriptions = (id) => get('/api/subscriptions/history', { userId: id })
export const getUserWishlist = (id, params) => get(`/api/wishlist/user/${id}`, clean(params))

// ---- loans
/** POST with a body: { userId, bookId, status, overdueOnly, page, size, sortBy, sortDirection, ... } */
export const searchLoans = (params) => post('/api/book-loans/search', clean(params))
export const issueBook = (userId, { bookId, dueDays, notes }) =>
  post(`/api/book-loans/checkout/user/${userId}`, clean({ bookId, dueDays, notes }))
export const updateLoan = (id, changes) => put(`/api/book-loans/${id}`, changes)
export const runOverdueUpdate = () => post('/api/book-loans/admin/update-overdue')

// ---- fines
export const searchFines = (params) => get('/api/fines', clean(params))
export const createFine = (fine) => post('/api/fines', fine)
/** amount in centimes. */
export const recordFinePayment = (id, amount, transactionId) =>
  post(`/api/fines/${id}/record-payment`, null, clean({ amount, transactionId }))
export const waiveFine = (fineId, waiverReason) => post('/api/fines/waive', clean({ fineId, waiverReason }))

// ---- reservations
export const searchReservations = (params) => get('/api/reservations', clean(params))
export const reserveForUser = (userId, { bookId, notes }) =>
  post(`/api/reservations/user/${userId}`, clean({ bookId, notes }))

// ---- payments
export const searchPayments = (params) => get('/api/payments', clean(params))

// ---- jobs and tools
export const getJobs = () => get('/api/admin/jobs')
export const runJob = (name) => post(`/api/admin/jobs/${encodeURIComponent(name)}/run`)
export const sendTestEmail = (to) => post('/api/admin/email/test', null, { to })
