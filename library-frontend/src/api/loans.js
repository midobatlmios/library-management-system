import client from './client.js'

/** Borrow a book. The backend picks the due date from the subscription plan. */
export const checkoutBook = (bookId) =>
  client.post('/api/book-loans/checkout', { bookId }).then((r) => r.data)

/** status: ACTIVE (not returned yet) | CHECKED_OUT | OVERDUE | RETURNED, or empty for all. Newest first. */
export const getMyLoans = ({ status, page = 0, size = 10 } = {}) =>
  client.get('/api/book-loans/my', { params: { status, page, size } }).then((r) => r.data)

/** Extends the due date (the backend decides by how much and how many times). */
export const renewLoan = (bookLoanId) =>
  client.post('/api/book-loans/renew', { bookLoanId }).then((r) => r.data)

/** Return a book. A late return creates an OVERDUE fine automatically. */
export const returnLoan = (bookLoanId) =>
  client.post('/api/book-loans/check-in', { bookLoanId }).then((r) => r.data)
