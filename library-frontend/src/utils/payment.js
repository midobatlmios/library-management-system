/**
 * What to do with the answer of subscribe / renew / pay fine
 * ({ paymentId, checkoutUrl, status, message }):
 *  - a checkoutUrl     -> leave the app for the payment page; it comes back to /payment/result
 *  - only a paymentId  -> already settled: show the result page
 *  - neither (free plan, active at once) -> show the message and go to `fallback`
 */
export function continueToPayment(payment, { navigate, toast, fallback = '/subscription' }) {
  if (payment.checkoutUrl) {
    window.location.assign(payment.checkoutUrl)
  } else if (payment.paymentId) {
    navigate(`/payment/result?paymentId=${encodeURIComponent(payment.paymentId)}`)
  } else {
    toast(payment.message || 'Done')
    navigate(fallback)
  }
}
