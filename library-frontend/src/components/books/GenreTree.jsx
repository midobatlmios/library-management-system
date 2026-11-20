import { useState } from 'react'
import { Collapse, IconButton, List, ListItemButton, ListItemText, Typography } from '@mui/material'
import ExpandLess from '@mui/icons-material/ExpandLess'
import ExpandMore from '@mui/icons-material/ExpandMore'

/** True when the genre or one of its sub-genres (any depth) is the selected one. */
function containsGenre(genre, id) {
  if (genre.id === id) return true
  return (genre.subGenres || []).some((g) => containsGenre(g, id))
}

function GenreItem({ genre, depth, selectedId, onSelect }) {
  const children = genre.subGenres || []
  // Open by default when the selected genre is inside this branch.
  const [open, setOpen] = useState(() => children.some((g) => containsGenre(g, selectedId)))

  return (
    <>
      <ListItemButton
        selected={genre.id === selectedId}
        onClick={() => onSelect(genre.id)}
        sx={{ pl: 1.5 + depth * 2, borderRadius: 1, py: 0.5 }}
      >
        <ListItemText
          primary={genre.name}
          slotProps={{ primary: { variant: 'body2' } }}
        />
        {genre.bookCount != null && (
          <Typography variant="caption" color="text.secondary" sx={{ ml: 1 }}>
            {genre.bookCount}
          </Typography>
        )}
        {children.length > 0 && (
          <IconButton
            size="small"
            edge="end"
            aria-label={open ? `Collapse ${genre.name}` : `Expand ${genre.name}`}
            onClick={(e) => {
              e.stopPropagation() // only open/close, don't select
              setOpen((o) => !o)
            }}
          >
            {open ? <ExpandLess fontSize="small" /> : <ExpandMore fontSize="small" />}
          </IconButton>
        )}
      </ListItemButton>
      {children.length > 0 && (
        <Collapse in={open} unmountOnExit>
          <List disablePadding>
            {children.map((g) => (
              <GenreItem key={g.id} genre={g} depth={depth + 1} selectedId={selectedId} onSelect={onSelect} />
            ))}
          </List>
        </Collapse>
      )}
    </>
  )
}

/**
 * The genre sidebar. genres = the tree from /api/genres/active/hierarchy.
 * selectedId = null means "All genres".
 */
export default function GenreTree({ genres, selectedId, onSelect }) {
  return (
    <List dense disablePadding aria-label="Genres">
      <ListItemButton selected={selectedId == null} onClick={() => onSelect(null)} sx={{ borderRadius: 1, py: 0.5 }}>
        <ListItemText primary="All genres" slotProps={{ primary: { variant: 'body2', fontWeight: 600 } }} />
      </ListItemButton>
      {genres.map((g) => (
        <GenreItem key={g.id} genre={g} depth={0} selectedId={selectedId} onSelect={onSelect} />
      ))}
    </List>
  )
}
