/**
 * Turns the flat genre list (each with parentGenreId) into tree order:
 * every genre followed by its children, with a depth for indenting.
 * Genres whose parent is missing are treated as top level.
 */
export function flattenGenreTree(genres) {
  const byParent = new Map()
  const ids = new Set(genres.map((g) => g.id))
  for (const g of genres) {
    const parent = g.parentGenreId != null && ids.has(g.parentGenreId) ? g.parentGenreId : null
    if (!byParent.has(parent)) byParent.set(parent, [])
    byParent.get(parent).push(g)
  }
  const sort = (list) =>
    [...list].sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0) || a.name.localeCompare(b.name))

  const result = []
  const seen = new Set()
  const walk = (parentId, depth) => {
    for (const g of sort(byParent.get(parentId) || [])) {
      if (seen.has(g.id)) continue // protects against a cycle in bad data
      seen.add(g.id)
      result.push({ ...g, depth })
      walk(g.id, depth + 1)
    }
  }
  walk(null, 0)
  return result
}

/** The ids of a genre and everything under it (a genre can't be moved under these). */
export function descendantIds(genres, id) {
  const out = new Set([id])
  let added = true
  while (added) {
    added = false
    for (const g of genres) {
      if (g.parentGenreId != null && out.has(g.parentGenreId) && !out.has(g.id)) {
        out.add(g.id)
        added = true
      }
    }
  }
  return out
}
