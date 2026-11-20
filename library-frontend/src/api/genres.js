import client from './client.js'

/** Active genres as a tree: top-level genres with their subGenres. */
export const getGenreHierarchy = () => client.get('/api/genres/active/hierarchy').then((r) => r.data)
