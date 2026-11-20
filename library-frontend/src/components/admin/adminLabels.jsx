import { Chip } from '@mui/material'

/** Status chips shared by the admin tables. */

const LOAN = {
  CHECKED_OUT: { label: 'Checked out', color: 'info' },
  OVERDUE: { label: 'Overdue', color: 'error' },
  RETURNED: { label: 'Returned', color: 'default' },
}
const FINE = {
  PENDING: { label: 'Unpaid', color: 'error' },
  PARTIALLY_PAID: { label: 'Partly paid', color: 'warning' },
  PAID: { label: 'Paid', color: 'success' },
  WAIVED: { label: 'Waived', color: 'default' },
}
const RESERVATION = {
  PENDING: { label: 'Waiting', color: 'info' },
  AVAILABLE: { label: 'Ready', color: 'success' },
  FULFILLED: { label: 'Picked up', color: 'default' },
  EXPIRED: { label: 'Expired', color: 'default' },
  CANCELLED: { label: 'Cancelled', color: 'default' },
}
const SUBSCRIPTION = {
  ACTIVE: { label: 'Active', color: 'success' },
  PENDING: { label: 'Waiting for payment', color: 'warning' },
  EXPIRED: { label: 'Expired', color: 'default' },
  CANCELLED: { label: 'Cancelled', color: 'default' },
}
const PAYMENT = {
  SUCCESS: { label: 'Paid', color: 'success' },
  PENDING: { label: 'Pending', color: 'warning' },
  FAILED: { label: 'Failed', color: 'error' },
  EXPIRED: { label: 'Expired', color: 'default' },
  CANCELLED: { label: 'Cancelled', color: 'default' },
}

export const FINE_TYPES = { OVERDUE: 'Late return', DAMAGE: 'Damaged', LOST: 'Lost', OTHER: 'Other' }

const chip = (map) =>
  function StatusChip({ status }) {
    const s = map[status] ?? { label: status, color: 'default' }
    return <Chip size="small" color={s.color} label={s.label} />
  }

export const LoanStatusChip = chip(LOAN)
export const FineStatusChip = chip(FINE)
export const ReservationStatusChip = chip(RESERVATION)
export const SubscriptionStatusChip = chip(SUBSCRIPTION)
export const PaymentStatusChip = chip(PAYMENT)

export const LOAN_STATUSES = Object.keys(LOAN)
export const FINE_STATUSES = Object.keys(FINE)
export const RESERVATION_STATUSES = Object.keys(RESERVATION)
export const PAYMENT_STATUSES = Object.keys(PAYMENT)
export const statusLabel = { LOAN, FINE, RESERVATION, PAYMENT }
