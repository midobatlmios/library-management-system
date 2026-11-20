import client from './client.js'

/** { isInWishlist: true|false } */
export const checkWishlist = (bookId) => client.get(`/api/wishlist/check/${bookId}`).then((r) => r.data)

export const addToWishlist = (bookId, notes) =>
  client.post(`/api/wishlist/add/${bookId}`, null, { params: { notes } }).then((r) => r.data)

export const removeFromWishlist = (bookId) =>
  client.delete(`/api/wishlist/remove/${bookId}`).then((r) => r.data)

export const getMyWishlist = ({ page = 0, size = 12 } = {}) =>
  client.get('/api/wishlist/my-wishlist', { params: { page, size } }).then((r) => r.data)

/** Empty notes clear them. */
export const updateWishlistNotes = (bookId, notes) =>
  client.put(`/api/wishlist/update-notes/${bookId}`, null, { params: { notes } }).then((r) => r.data)
