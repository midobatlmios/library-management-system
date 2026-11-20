import { useMemo, useState } from 'react'
import { Button, Chip, Stack, TextField, Typography } from '@mui/material'
import { Link as RouterLink } from 'react-router-dom'
import { getAllUsers } from '../../api/admin.js'
import AdminTable from '../../components/admin/AdminTable.jsx'
import PageTitle from '../../components/enamel/PageTitle.jsx'
import { formatDate } from '../../utils/format.js'
import { useAsync } from '../../utils/useAsync.js'

const PAGE_SIZE = 20

/** All users. The backend returns them as one list, so search and paging happen in the browser. */
export default function AdminUsersPage() {
  const users = useAsync(getAllUsers)
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase()
    const list = users.data || []
    return q
      ? list.filter((u) => [u.fullName, u.email, u.phone].some((v) => v && v.toLowerCase().includes(q)))
      : list
  }, [users.data, search])

  const columns = [
    {
      key: 'name',
      label: 'User',
      render: (u) => (
        <div className="min-w-48">
          <Typography variant="body2" sx={{ fontWeight: 600 }}>
            {u.fullName}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            {u.email}
          </Typography>
        </div>
      ),
    },
    {
      key: 'role',
      label: 'Role',
      render: (u) => (u.role === 'ROLE_ADMIN' ? <Chip size="small" color="secondary" label="Admin" /> : 'Member'),
    },
    { key: 'authProvider', label: 'Sign-in', render: (u) => (u.authProvider === 'GOOGLE' ? 'Google' : 'Email') },
    {
      key: 'verified',
      label: 'Email',
      render: (u) =>
        u.verified ? <Chip size="small" color="success" variant="outlined" label="Verified" /> : <Chip size="small" color="warning" label="Not verified" />,
    },
    { key: 'createdAt', label: 'Joined', render: (u) => <span className="whitespace-nowrap">{formatDate(u.createdAt)}</span> },
    {
      key: 'actions',
      label: '',
      align: 'right',
      render: (u) => (
        <Button size="small" component={RouterLink} to={`/admin/users/${u.id}`}>
          Details
        </Button>
      ),
    },
  ]

  return (
    <Stack spacing={3}>
      <PageTitle aside={users.data && `${users.data.length} accounts`}>Users</PageTitle>
      <TextField
        size="small"
        label="Search name, email or phone"
        value={search}
        onChange={(e) => {
          setSearch(e.target.value)
          setPage(0)
        }}
        sx={{ maxWidth: 420 }}
      />
      <AdminTable
        columns={columns}
        rows={users.data ? filtered.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE) : undefined}
        loading={users.loading}
        error={users.error}
        empty="No users match."
        paging={users.data && { page, size: PAGE_SIZE, total: filtered.length, onPageChange: setPage }}
      />
    </Stack>
  )
}
