import { useState } from 'react'
import { Alert, Button, Card, CardContent, Chip, Stack, TextField, Tooltip, Typography } from '@mui/material'
import PlayArrowIcon from '@mui/icons-material/PlayArrow'
import { useSelector } from 'react-redux'
import { getJobs, runJob, sendTestEmail } from '../../api/admin.js'
import AdminTable from '../../components/admin/AdminTable.jsx'
import { useToast } from '../../components/feedback/toastContext.js'
import { selectUser } from '../../store/authSlice.js'
import { formatDateTime } from '../../utils/format.js'
import { useAsync } from '../../utils/useAsync.js'
import PageTitle from '../../components/enamel/PageTitle.jsx'

/** Send a real email now, to check the mail settings (secrets.properties). */
function EmailTestCard() {
  const user = useSelector(selectUser)
  const toast = useToast()
  const [to, setTo] = useState(user?.email ?? '')
  const [result, setResult] = useState(null)
  const [busy, setBusy] = useState(false)

  const handleSend = async (e) => {
    e.preventDefault()
    setBusy(true)
    setResult(null)
    try {
      const r = await sendTestEmail(to.trim())
      setResult({ ok: true, message: r.message })
      toast(r.message)
    } catch (err) {
      setResult({ ok: false, message: err.message })
    } finally {
      setBusy(false)
    }
  }

  return (
    <Card variant="outlined">
      <CardContent>
        <Stack spacing={2}>
          <div>
            <Typography variant="h6" component="h2" sx={{ fontWeight: 700 }}>
              Email test
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Sends a test email right away and shows whether the mail server accepted it.
            </Typography>
          </div>
          <form onSubmit={handleSend} className="flex flex-col gap-2 sm:flex-row">
            <TextField size="small" type="email" label="Send to" value={to} onChange={(e) => setTo(e.target.value)} className="flex-1" required />
            <Button type="submit" variant="contained" disabled={busy || !to.trim()}>
              {busy ? 'Sending…' : 'Send test email'}
            </Button>
          </form>
          {result && <Alert severity={result.ok ? 'success' : 'error'}>{result.message}</Alert>}
        </Stack>
      </CardContent>
    </Card>
  )
}

/** /admin/jobs — the scheduled jobs (step 13), with "Run now", and the email test. */
export default function AdminJobsPage() {
  const toast = useToast()
  const jobs = useAsync(getJobs)
  const [running, setRunning] = useState(null)

  const handleRun = async (name) => {
    setRunning(name)
    try {
      const status = await runJob(name)
      if (status.lastError) toast(`${name} failed: ${status.lastError}`, 'error')
      else toast(`${name} done${status.lastResultCount != null ? `: ${status.lastResultCount} item(s)` : ''}`)
      jobs.reload()
    } catch (e) {
      toast(e.message, 'error')
    } finally {
      setRunning(null)
    }
  }

  const columns = [
    {
      key: 'name',
      label: 'Job',
      render: (j) => (
        <div className="min-w-56">
          <Typography variant="body2" sx={{ fontWeight: 600 }}>
            {j.name} {j.running && <Chip size="small" color="info" label="Running" sx={{ ml: 1 }} />}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            {j.description}
          </Typography>
        </div>
      ),
    },
    {
      key: 'schedule',
      label: 'Schedule',
      render: (j) => (
        <div className="whitespace-nowrap">
          <code>{j.schedule}</code>
          {j.nextRun && (
            <Typography variant="caption" component="p" color="text.secondary">
              Next: {formatDateTime(j.nextRun)}
            </Typography>
          )}
        </div>
      ),
    },
    {
      key: 'last',
      label: 'Last run',
      render: (j) =>
        j.lastStartedAt ? (
          <div className="whitespace-nowrap">
            {formatDateTime(j.lastStartedAt)}
            <Typography variant="caption" component="p" color="text.secondary">
              {j.lastDurationMs != null && `${j.lastDurationMs} ms`}
              {j.lastResultCount != null && ` · ${j.lastResultCount} item(s)`}
            </Typography>
          </div>
        ) : (
          'Never'
        ),
    },
    {
      key: 'result',
      label: 'Result',
      render: (j) =>
        j.lastError ? (
          <Tooltip title={j.lastError}>
            <Chip size="small" color="error" label="Error" />
          </Tooltip>
        ) : j.lastFinishedAt ? (
          <Chip size="small" color="success" variant="outlined" label="OK" />
        ) : (
          ''
        ),
    },
    { key: 'totalRuns', label: 'Runs', align: 'right' },
    {
      key: 'actions',
      label: '',
      align: 'right',
      render: (j) => (
        <Button
          size="small"
          startIcon={<PlayArrowIcon />}
          onClick={() => handleRun(j.name)}
          disabled={running !== null || j.running}
          className="whitespace-nowrap"
        >
          {running === j.name ? 'Running…' : 'Run now'}
        </Button>
      ),
    },
  ]

  return (
    <Stack spacing={3}>
      <div className="flex flex-wrap items-center justify-between gap-2">
        <PageTitle>Jobs & tools</PageTitle>
        <Button onClick={jobs.reload}>Refresh</Button>
      </div>
      <AdminTable columns={columns} rows={jobs.data} rowKey="name" loading={jobs.loading} error={jobs.error} empty="No jobs." />
      <EmailTestCard />
    </Stack>
  )
}
