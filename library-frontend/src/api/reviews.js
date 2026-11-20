import client from './client.js'

/**
 * Reviews of a book (public).
 * filter: ALL | VERIFIED_ONLY | TOP_HELPFUL | BY_RATING (then rating 1-5 is required)
 */
export const getBookReviews = (bookId, { filter = 'ALL', rating, page = 0, size = 5 } = {}) =>
  client
    .get(`/api/reviews/book/${bookId}`, { params: { filter, rating, page, size } })
    .then((r) => r.data)

/** { averageRating, totalReviews, ratingDistribution: { "1": n, ... "5": n }, verifiedReaderReviews } */
export const getBookRatingStats = (bookId) =>
  client.get(`/api/reviews/book/${bookId}/statistics`).then((r) => r.data)

export const markReviewHelpful = (reviewId) =>
  client.post(`/api/reviews/${reviewId}/helpful`).then((r) => r.data)

/** { canReview, reason } — reason says why not (e.g. already reviewed). */
export const canReviewBook = (bookId) => client.get(`/api/reviews/can-review/${bookId}`).then((r) => r.data)

/** review: { rating 1-5, title (optional), reviewText 10-2000 chars } */
export const createReview = (bookId, review) =>
  client.post('/api/reviews', { bookId, ...review }).then((r) => r.data)

export const updateReview = (reviewId, review) =>
  client.put(`/api/reviews/${reviewId}`, review).then((r) => r.data)

export const deleteReview = (reviewId) => client.delete(`/api/reviews/${reviewId}`).then((r) => r.data)

export const getMyReviews = ({ page = 0, size = 10 } = {}) =>
  client.get('/api/reviews/my-reviews', { params: { page, size } }).then((r) => r.data)
