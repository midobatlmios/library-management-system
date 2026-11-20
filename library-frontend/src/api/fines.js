import client from './client.js'

export const getMyFines = ({ page = 0, size = 10 } = {}) =>
  client.get('/api/fines/my', { params: { page, size } }).then((r) => r.data)

/** { total, totalInMajorUnits, currency } */
export const getMyUnpaidTotal = () => client.get('/api/fines/my/total-unpaid').then((r) => r.data)

/** Starts a payment: { paymentId, checkoutUrl, amountInMajorUnits, currency, status } */
export const payFine = (id) => client.post(`/api/fines/${id}/pay`).then((r) => r.data)
