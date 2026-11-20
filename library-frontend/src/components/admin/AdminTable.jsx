import {
  Alert,
  LinearProgress,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  Typography,
} from '@mui/material'

/**
 * A simple data table for the admin pages.
 *   columns: [{ key, label, render?(row), align?, width? }]
 *   rows:    array of objects
 *   paging (optional): { page (0-based), size, total, onPageChange(page) }
 * Scrolls sideways on phones instead of squeezing the columns.
 */
export default function AdminTable({ columns, rows, rowKey = 'id', loading, error, empty = 'Nothing found.', paging }) {
  return (
    <Paper variant="outlined">
      {error && (
        <Alert severity="error" square>
          {error}
        </Alert>
      )}
      <LinearProgress sx={{ visibility: loading ? 'visible' : 'hidden' }} />
      <TableContainer>
        <Table size="small">
          <TableHead>
            <TableRow>
              {columns.map((c) => (
                <TableCell key={c.key} align={c.align} sx={{ fontWeight: 600, whiteSpace: 'nowrap', width: c.width }}>
                  {c.label}
                </TableCell>
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {rows?.map((row) => (
              <TableRow key={typeof rowKey === 'function' ? rowKey(row) : row[rowKey]} hover>
                {columns.map((c) => (
                  <TableCell key={c.key} align={c.align}>
                    {c.render ? c.render(row) : row[c.key]}
                  </TableCell>
                ))}
              </TableRow>
            ))}
            {rows && rows.length === 0 && !loading && (
              <TableRow>
                <TableCell colSpan={columns.length}>
                  <Typography color="text.secondary" sx={{ py: 2, textAlign: 'center' }}>
                    {empty}
                  </Typography>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>
      {paging && (
        <TablePagination
          component="div"
          count={paging.total}
          page={paging.page}
          rowsPerPage={paging.size}
          rowsPerPageOptions={[paging.size]}
          onPageChange={(_, p) => paging.onPageChange(p)}
        />
      )}
    </Paper>
  )
}
