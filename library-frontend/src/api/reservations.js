import client from './client.js'

/** Join the waiting list for a book that has no copy available. */
export const reserveBook = (bookId) => client.post('/api/reservations', { bookId }).then((r) => r.data)

/** activeOnly: true = only PENDING and AVAILABLE. Newest first. */
export const getMyReservations = ({ activeOnly, page = 0, size = 10 } = {}) =>
  client.get('/api/reservations/my', { params: { activeOnly, page, size } }).then((r) => r.data)

export const cancelReservation = (id) => client.delete(`/api/reservations/${id}`).then((r) => r.data)

/** Borrow the copy that is being held for you (creates a loan). */
export const fulfillReservation = (id) => client.post(`/api/reservations/${id}/fulfill`).then((r) => r.data)
