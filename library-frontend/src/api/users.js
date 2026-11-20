import client from './client.js'

/** The logged-in user, fresh from the database. */
export const getProfile = () => client.get('/api/users/profile').then((r) => r.data)
