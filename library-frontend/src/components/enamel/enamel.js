/**
 * Each genre gets its own enamel colour, always the same one (picked from the
 * genre's id), so a member learns "green plates are Programming" by sight.
 * Cobalt is kept for the site's own signs (navigation, search), not genres.
 * All colours carry white lettering at 4.5:1 or better.
 */
const GENRE_ENAMELS = [
  '#1d6b49', // bottle green
  '#a8261f', // signal red
  '#8a5c00', // ochre (darkened for white lettering)
  '#1b1d23', // black
  '#0d6672', // teal
  '#5b2a6e', // plum
  '#132c76', // deep cobalt
  '#7a3412', // brick
]

export function genreEnamel(genreId, genreName = '') {
  let n = Number(genreId)
  if (!Number.isFinite(n)) {
    // No id: derive a stable number from the name.
    n = [...genreName].reduce((h, c) => (h * 31 + c.charCodeAt(0)) >>> 0, 7)
  }
  return GENRE_ENAMELS[Math.abs(n) % GENRE_ENAMELS.length]
}
