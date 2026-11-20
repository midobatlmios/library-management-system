import client from './client.js'

/** Always 200. { success: true } when the logged-in user has a valid subscription. */
export const checkSubscription = () => client.get('/api/subscriptions/check').then((r) => r.data)

/** Active plans for the public plans page, cheapest first. */
export const getPlans = () =>
  client
    .get('/api/subscription-plans', { params: { active: true, size: 50, sortBy: 'price', sortDirection: 'ASC' } })
    .then((r) => r.data.content)

/**
 * Starts a subscription. Answers { paymentId, checkoutUrl, amountInMajorUnits, currency, status }.
 * Free plans answer status "SUCCESS" and no checkoutUrl (already active).
 */
export const subscribe = (planId) =>
  client.post('/api/subscriptions/subscribe', { planId }).then((r) => r.data)

/** The current subscription, or null when there is none (the backend answers 404). */
export const getActiveSubscription = () =>
  client
    .get('/api/subscriptions/user/active')
    .then((r) => r.data)
    .catch((e) => {
      if (e.status === 404) return null
      throw e
    })

/** All my subscriptions, newest first. */
export const getSubscriptionHistory = () => client.get('/api/subscriptions/history').then((r) => r.data)

/** Pays for the next period. Same answer as subscribe(). */
export const renewSubscription = (id) => client.post(`/api/subscriptions/renew/${id}`).then((r) => r.data)

export const cancelSubscription = (id, reason) =>
  client.post(`/api/subscriptions/cancel/${id}`, null, { params: { reason } }).then((r) => r.data)
