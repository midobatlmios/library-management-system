import client from './client.js'

/** One payment: { paymentId, status: PENDING|SUCCESS|FAILED|EXPIRED|CANCELLED, description, amountInMajorUnits, ... } */
export const getPayment = (paymentId) =>
  client.get(`/api/payments/${encodeURIComponent(paymentId)}`).then((r) => r.data)

/** Stop a payment that is still PENDING. */
export const cancelPayment = (paymentId) =>
  client.post(`/api/payments/${encodeURIComponent(paymentId)}/cancel`).then((r) => r.data)

export const getMyPayments = ({ page = 0, size = 10 } = {}) =>
  client.get('/api/payments/my', { params: { page, size } }).then((r) => r.data)
