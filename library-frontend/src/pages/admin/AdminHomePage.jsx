import { useCallback, useState } from 'react'
import { Alert, Stack, ToggleButton, ToggleButtonGroup, Typography } from '@mui/material'
import * as admin from '../../api/admin.js'
import { getGenreHierarchy } from '../../api/genres.js'
import StatCard from '../../components/admin/StatCard.jsx'
import BarList from '../../components/charts/BarList.jsx'
import ChartCard, { ChartTable } from '../../components/charts/ChartCard.jsx'
import ColumnChart from '../../components/charts/ColumnChart.jsx'
import LineChart from '../../components/charts/LineChart.jsx'
import { shortDate } from '../../components/charts/useWidth.js'
import PageTitle from '../../components/enamel/PageTitle.jsx'
import { formatMoney } from '../../utils/format.js'
import { useAsync } from '../../utils/useAsync.js'

const RANGES = [7, 30, 90]

/**
 * Loads every "right now" statistic at the same time. allSettled: if one
 * endpoint fails, the other tiles still show (that one shows "—").
 */
async function loadTotals() {
  const calls = {
    books: admin.getBookStats(),
    loans: admin.getLoanStats(),
    finesCollected: admin.getFinesCollected(),
    finesOutstanding: admin.getFinesOutstanding(),
    payments: admin.getPaymentStats(),
    plans: admin.getPlanStats(),
    users: admin.getUserStats(),
    reviews: admin.getReviewStats(),
    genres: getGenreHierarchy(),
    planList: admin.getAllPlans(),
  }
  const keys = Object.keys(calls)
  const results = await Promise.allSettled(Object.values(calls))
  const data = {}
  const failed = []
  results.forEach((r, i) => {
    if (r.status === 'fulfilled') data[keys[i]] = r.value
    else failed.push(keys[i])
  })
  return { ...data, failed }
}

const n = (v) => (v == null ? null : Number(v).toLocaleString('en-GB'))
const sum = (rows, key) => rows.reduce((s, r) => s + (Number(r[key]) || 0), 0)

export default function AdminHomePage() {
  const [days, setDays] = useState(30)
  const totals = useAsync(loadTotals)
  const loadDaily = useCallback(() => admin.getDailyActivity(days), [days])
  const daily = useAsync(loadDaily)

  const s = totals.data
  const d = daily.data
  const rows = d?.days ?? []
  const currency = d?.currency ?? 'MAD'
  const busy = totals.loading && !s
  const period = `last ${days} days`

  // Plan statistics are keyed by plan code; show plan titles.
  const planTitle = Object.fromEntries((s?.planList ?? []).map((p) => [p.planCode, p.title]))
  const byPlan = Object.entries(s?.plans?.activeSubscriptionsByPlan ?? {}).map(([code, value]) => ({
    name: planTitle[code] ?? code,
    value,
  }))
  const byGenre = (s?.genres ?? []).map((g) => ({ name: g.name, value: g.bookCount ?? 0 }))

  return (
    <Stack spacing={3}>
      <PageTitle>Dashboard</PageTitle>
      {s?.failed.length > 0 && <Alert severity="warning">Could not load: {s.failed.join(', ')}</Alert>}

      {/* One filter, above everything it scopes: the period tiles and the two daily charts. */}
      <div className="flex flex-wrap items-center gap-3">
        <span className="sign-caps text-[12px]" style={{ color: 'var(--mui-palette-text-secondary)' }}>
          Period
        </span>
        <ToggleButtonGroup size="small" exclusive value={days} onChange={(_, v) => v && setDays(v)} aria-label="Period">
          {RANGES.map((r) => (
            <ToggleButton key={r} value={r}>
              Last {r} days
            </ToggleButton>
          ))}
        </ToggleButtonGroup>
      </div>
      {daily.error && <Alert severity="error">{daily.error}</Alert>}

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label={`Checkouts, ${period}`} loading={!d} value={d && n(sum(rows, 'checkouts'))} to="/admin/loans" lines={[d && `${n(sum(rows, 'returns'))} books returned`]} />
        <StatCard label={`Collected, ${period}`} loading={!d} value={d && formatMoney(sum(rows, 'collected'), currency)} to="/admin/payments" lines={[s?.payments && `${formatMoney(s.payments.collectedTotal, currency)} since the start`]} />
        <StatCard
          label="Active loans now"
          to="/admin/loans"
          loading={busy}
          value={n(s?.loans?.activeLoans)}
          lines={[s?.loans && `${n(s.loans.overdueLoans)} overdue`, s?.loans?.lateButNotMarked > 0 && `${n(s.loans.lateButNotMarked)} late, not yet marked`]}
        />
        <StatCard
          label="Fines outstanding"
          to="/admin/fines"
          loading={busy}
          value={s?.finesOutstanding && formatMoney(s.finesOutstanding.totalInMajorUnits, s.finesOutstanding.currency)}
          lines={[s?.finesCollected && `${formatMoney(s.finesCollected.totalInMajorUnits, s.finesCollected.currency)} collected`]}
        />
      </div>

      <div className="grid grid-cols-1 gap-4 xl:grid-cols-2">
        <ChartCard
          title="Loans per day"
          subtitle={`Books borrowed and returned, ${period}`}
          loading={daily.loading && Boolean(d)}
          table={
            d && (
              <ChartTable
                columns={[
                  { key: 'date', label: 'Day', format: shortDate },
                  { key: 'checkouts', label: 'Borrowed', align: 'right' },
                  { key: 'returns', label: 'Returned', align: 'right' },
                ]}
                rows={[...rows].reverse()}
              />
            )
          }
        >
          {d ? (
            <LineChart
              rows={rows}
              label={`Loans per day, ${period}`}
              series={[
                { key: 'checkouts', label: 'Borrowed', color: 'var(--series-1)' },
                { key: 'returns', label: 'Returned', color: 'var(--series-2)' },
              ]}
            />
          ) : (
            <div style={{ height: 262 }} />
          )}
        </ChartCard>

        <ChartCard
          title={`Money collected per day (${currency})`}
          subtitle={`Successful payments, subscriptions and fines, ${period}`}
          loading={daily.loading && Boolean(d)}
          table={
            d && (
              <ChartTable
                columns={[
                  { key: 'date', label: 'Day', format: shortDate },
                  { key: 'collected', label: `Collected (${currency})`, align: 'right', format: (v) => Number(v).toFixed(2) },
                ]}
                rows={[...rows].reverse()}
              />
            )
          }
        >
          {d ? (
            <ColumnChart
              rows={rows}
              valueKey="collected"
              label={`Money collected per day, ${period}`}
              format={(v) => Number(v).toLocaleString('en-US', { maximumFractionDigits: 2 })}
            />
          ) : (
            <div style={{ height: 230 }} />
          )}
        </ChartCard>

        <ChartCard title="Active subscriptions by plan" subtitle="Members with a valid subscription today">
          <BarList items={byPlan} empty="No active subscriptions yet." />
        </ChartCard>

        <ChartCard title="Books by genre" subtitle="Active books, sub-genres included">
          <BarList items={byGenre} empty="No genres yet." />
        </ChartCard>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Books in the catalog" to="/admin/books" loading={busy} value={n(s?.books?.totalActiveBooks)} lines={[s?.books && `${n(s.books.totalAvailableBooks)} with a copy on the shelf`]} />
        <StatCard
          label="Active subscriptions"
          to="/admin/plans"
          loading={busy}
          value={n(s?.plans?.activeSubscriptions)}
          lines={[s?.plans && `${n(s.plans.activePlans)} active plans, ${n(s.plans.inactivePlans)} inactive`]}
        />
        <StatCard label="Users" to="/admin/users" loading={busy} value={n(s?.users?.totalUsers)} />
        <StatCard
          label="Reviews"
          loading={busy}
          value={n(s?.reviews?.totalReviews)}
          lines={[
            s?.reviews?.averageRating != null && `Average ${Number(s.reviews.averageRating).toFixed(1)} / 5`,
            s?.reviews && `${n(s.reviews.reviewsLast30Days)} in the last 30 days`,
          ]}
        />
      </div>
      <Typography variant="caption" color="text.secondary">
        Tip: every chart has a Table view with all its values. Charts respond to the mouse and, after Tab, to the arrow keys.
      </Typography>
    </Stack>
  )
}
