import { useState } from 'react'
import {
  Alert,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  IconButton,
  MenuItem,
  Stack,
  Switch,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import EditIcon from '@mui/icons-material/Edit'
import RestoreIcon from '@mui/icons-material/Restore'
import BlockIcon from '@mui/icons-material/Block'
import * as admin from '../../api/admin.js'
import AdminTable from '../../components/admin/AdminTable.jsx'
import ConfirmDialog from '../../components/feedback/ConfirmDialog.jsx'
import { useToast } from '../../components/feedback/toastContext.js'
import { formatMoney } from '../../utils/format.js'
import { useAsync } from '../../utils/useAsync.js'
import PageTitle from '../../components/enamel/PageTitle.jsx'

const CYCLES = { MONTHLY: 'Monthly', YEARLY: 'Yearly', ONE_TIME: 'One time' }

function PlanFormDialog({ plan, onSubmit, onClose }) {
  const [form, setForm] = useState({
    title: plan?.title ?? '',
    planCode: plan?.planCode ?? '',
    description: plan?.description ?? '',
    price: plan?.price ?? '',
    billingCycle: plan?.billingCycle ?? 'MONTHLY',
    durationInDays: plan?.durationInDays ?? 30,
    trialDays: plan?.trialDays ?? 0,
    maxBooksAllowed: plan?.maxBooksAllowed ?? 3,
    maxBorrowDays: plan?.maxBorrowDays ?? 14,
    features: (plan?.features || []).join('\n'),
    autoRenew: plan?.autoRenew ?? false,
  })
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  const codeError = form.planCode !== '' && !/^[A-Z0-9_]{2,50}$/.test(form.planCode)
  const valid =
    form.title.trim() &&
    form.price !== '' &&
    Number(form.price) >= 0 &&
    Number(form.durationInDays) >= 1 &&
    Number(form.maxBooksAllowed) >= 1 &&
    Number(form.maxBorrowDays) >= 1 &&
    !codeError

  const handleSubmit = async () => {
    setError(null)
    setBusy(true)
    try {
      await onSubmit({
        ...(plan || {}),
        title: form.title.trim(),
        planCode: form.planCode || null, // empty: the backend makes one from the title
        description: form.description.trim() || null,
        price: Number(form.price),
        billingCycle: form.billingCycle,
        durationInDays: Number(form.durationInDays),
        trialDays: Number(form.trialDays) || 0,
        maxBooksAllowed: Number(form.maxBooksAllowed),
        maxBorrowDays: Number(form.maxBorrowDays),
        features: form.features.split('\n').map((f) => f.trim()).filter(Boolean),
        autoRenew: form.autoRenew,
      })
    } catch (e) {
      setError(e.message)
      setBusy(false)
    }
  }

  return (
    <Dialog open onClose={busy ? undefined : onClose} fullWidth maxWidth="md">
      <DialogTitle>{plan ? `Edit "${plan.title}"` : 'New plan'}</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          {error && <Alert severity="error">{error}</Alert>}
          {plan && (
            <Alert severity="info">
              Changes apply to new subscriptions. Existing subscriptions keep the price and limits they were bought with.
            </Alert>
          )}
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <TextField label="Title" value={form.title} onChange={set('title')} required />
            <TextField
              label="Plan code"
              value={form.planCode}
              onChange={(e) => setForm({ ...form, planCode: e.target.value.toUpperCase() })}
              error={codeError}
              helperText={codeError ? 'A-Z, 0-9 and _ only' : 'Optional: made from the title when empty'}
            />
            <TextField
              label="Price (MAD)"
              type="number"
              value={form.price}
              onChange={set('price')}
              required
              slotProps={{ htmlInput: { min: 0, step: '0.01' } }}
              helperText="0 makes a free plan"
            />
            <TextField select label="Billing cycle" value={form.billingCycle} onChange={set('billingCycle')}>
              {Object.entries(CYCLES).map(([v, l]) => (
                <MenuItem key={v} value={v}>
                  {l}
                </MenuItem>
              ))}
            </TextField>
            <TextField label="Duration (days)" type="number" value={form.durationInDays} onChange={set('durationInDays')} required slotProps={{ htmlInput: { min: 1 } }} />
            <TextField label="Free trial (days)" type="number" value={form.trialDays} onChange={set('trialDays')} slotProps={{ htmlInput: { min: 0 } }} />
            <TextField label="Books at a time" type="number" value={form.maxBooksAllowed} onChange={set('maxBooksAllowed')} required slotProps={{ htmlInput: { min: 1 } }} />
            <TextField label="Days per loan" type="number" value={form.maxBorrowDays} onChange={set('maxBorrowDays')} required slotProps={{ htmlInput: { min: 1 } }} />
            <TextField label="Description" value={form.description} onChange={set('description')} multiline minRows={2} className="sm:col-span-2" slotProps={{ htmlInput: { maxLength: 500 } }} />
            <TextField
              label="Features (one per line)"
              value={form.features}
              onChange={set('features')}
              multiline
              minRows={3}
              className="sm:col-span-2"
            />
            <FormControlLabel
              control={<Switch checked={form.autoRenew} onChange={(e) => setForm({ ...form, autoRenew: e.target.checked })} />}
              label="Auto-renew by default"
            />
          </div>
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={busy}>
          Cancel
        </Button>
        <Button variant="contained" onClick={handleSubmit} disabled={busy || !valid}>
          {busy ? 'Saving…' : plan ? 'Save changes' : 'Create plan'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

export default function AdminPlansPage() {
  const toast = useToast()
  const plans = useAsync(admin.getAllPlans)
  const stats = useAsync(admin.getPlanStats)
  const [editing, setEditing] = useState(null)
  const [deactivating, setDeactivating] = useState(null)

  const act = async (fn, message) => {
    try {
      await fn()
      toast(message)
      plans.reload()
      stats.reload()
    } catch (e) {
      toast(e.message, 'error')
    }
  }

  const handleSave = async (dto) => {
    if (editing === 'new') await admin.createPlan(dto)
    else await admin.updatePlan(editing.id, dto)
    toast(editing === 'new' ? 'Plan created' : 'Plan saved')
    setEditing(null)
    plans.reload()
  }

  const subscribers = stats.data?.activeSubscriptionsByPlan || {}

  const columns = [
    {
      key: 'title',
      label: 'Plan',
      render: (p) => (
        <div className="min-w-40">
          <Typography variant="body2" sx={{ fontWeight: 600 }}>
            {p.title}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            {p.planCode}
          </Typography>
        </div>
      ),
    },
    {
      key: 'price',
      label: 'Price',
      render: (p) => (
        <span className="whitespace-nowrap">
          {Number(p.price) === 0 ? 'Free' : formatMoney(p.price, p.currency)} · {CYCLES[p.billingCycle] ?? p.billingCycle}
        </span>
      ),
    },
    { key: 'limits', label: 'Limits', render: (p) => `${p.maxBooksAllowed} books · ${p.maxBorrowDays} days · ${p.durationInDays}-day period` },
    // The statistics are keyed by plan code.
    { key: 'subs', label: 'Active subscribers', align: 'right', render: (p) => subscribers[p.planCode] ?? 0 },
    {
      key: 'active',
      label: 'Status',
      render: (p) => (p.active === false ? <Chip size="small" label="Inactive" /> : <Chip size="small" color="success" variant="outlined" label="Active" />),
    },
    {
      key: 'actions',
      label: '',
      align: 'right',
      render: (p) => (
        <div className="flex justify-end whitespace-nowrap">
          <Tooltip title="Edit">
            <IconButton size="small" onClick={() => setEditing(p)}>
              <EditIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          {p.active === false ? (
            <Tooltip title="Reactivate (show on the plans page again)">
              <IconButton size="small" onClick={() => act(() => admin.updatePlan(p.id, { ...p, active: true }), 'Plan reactivated')}>
                <RestoreIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          ) : (
            <Tooltip title="Deactivate (no new subscriptions; current ones continue)">
              <IconButton size="small" onClick={() => setDeactivating(p)}>
                <BlockIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          )}
        </div>
      ),
    },
  ]

  return (
    <Stack spacing={3}>
      <div className="flex flex-wrap items-center justify-between gap-2">
        <PageTitle>Plans</PageTitle>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setEditing('new')}>
          New plan
        </Button>
      </div>
      {stats.data && (
        <Typography color="text.secondary">
          {stats.data.activeSubscriptions} active subscription{stats.data.activeSubscriptions === 1 ? '' : 's'} ·{' '}
          {stats.data.activePlans} active plan{stats.data.activePlans === 1 ? '' : 's'}, {stats.data.inactivePlans} inactive
        </Typography>
      )}
      <AdminTable columns={columns} rows={plans.data} loading={plans.loading} error={plans.error} empty="No plans yet." />
      <ConfirmDialog
        open={Boolean(deactivating)}
        title="Deactivate this plan?"
        message={deactivating && `"${deactivating.title}" is removed from the plans page, so nobody can subscribe or renew with it. Current subscriptions continue until they end. You can reactivate it any time.`}
        confirmLabel="Deactivate"
        cancelLabel="Cancel"
        danger
        onConfirm={() => act(() => admin.deactivatePlan(deactivating.id), 'Plan deactivated')}
        onClose={() => setDeactivating(null)}
      />
      {editing && (
        <PlanFormDialog
          key={editing === 'new' ? 'new' : editing.id}
          plan={editing === 'new' ? null : editing}
          onSubmit={handleSave}
          onClose={() => setEditing(null)}
        />
      )}
    </Stack>
  )
}
