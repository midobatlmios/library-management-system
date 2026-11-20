import { useEffect, useState } from 'react'
import { Autocomplete, TextField } from '@mui/material'
import { getAllUsers, searchBooksAdmin } from '../../api/admin.js'

/**
 * Choose a user by name or email. All users are loaded once (the backend
 * returns them as one list) and filtered in the browser.
 * value / onChange work with the user object (or null).
 */
export function UserPicker({ value, onChange, label = 'User', required, size }) {
  const [users, setUsers] = useState(null)
  useEffect(() => {
    let cancelled = false
    getAllUsers()
      .then((u) => !cancelled && setUsers(u))
      .catch(() => !cancelled && setUsers([]))
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <Autocomplete
      options={users || []}
      loading={!users}
      value={value}
      onChange={(_, v) => onChange(v)}
      getOptionLabel={(u) => `${u.fullName} <${u.email}>`}
      isOptionEqualToValue={(a, b) => a.id === b.id}
      size={size}
      renderInput={(params) => <TextField {...params} label={label} required={required} />}
    />
  )
}

/**
 * Choose a book by title, author or ISBN. Searches the backend as you type
 * (300 ms after the last key), active books only.
 */
export function BookPicker({ value, onChange, label = 'Book', required, size }) {
  const [input, setInput] = useState('')
  const [options, setOptions] = useState([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    let cancelled = false
    const timer = setTimeout(() => {
      setLoading(true)
      searchBooksAdmin({ search: input, includeInactive: false, size: 20, sortBy: 'title', sortDirection: 'ASC' })
        .then((r) => !cancelled && setOptions(r.content))
        .catch(() => !cancelled && setOptions([]))
        .finally(() => !cancelled && setLoading(false))
    }, 300)
    return () => {
      cancelled = true
      clearTimeout(timer)
    }
  }, [input])

  return (
    <Autocomplete
      options={value && !options.some((o) => o.id === value.id) ? [value, ...options] : options}
      loading={loading}
      value={value}
      onChange={(_, v) => onChange(v)}
      onInputChange={(_, v, reason) => reason === 'input' && setInput(v)}
      filterOptions={(x) => x} // the backend already filtered
      getOptionLabel={(b) => `${b.title} — ${b.author} (${b.availableCopies}/${b.totalCopies} available)`}
      isOptionEqualToValue={(a, b) => a.id === b.id}
      size={size}
      renderInput={(params) => <TextField {...params} label={label} required={required} />}
    />
  )
}
