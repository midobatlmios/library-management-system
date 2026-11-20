import client from './client.js'

// /auth/** on the backend. Login, signup and Google return { jwt, message, title, user }.

export const login = (email, password) =>
  client.post('/auth/login', { email, password }).then((r) => r.data)

export const signup = ({ fullName, email, phone, password }) =>
  client.post('/auth/signup', { fullName, email, phone: phone || null, password }).then((r) => r.data)

/** credential = the ID token Google's button gives us. */
export const loginWithGoogle = (credential) =>
  client.post('/auth/google', { idToken: credential }).then((r) => r.data)

export const forgotPassword = (email) =>
  client.post('/auth/forgot-password', { email }).then((r) => r.data)

export const resetPassword = (token, password) =>
  client.post('/auth/reset-password', { token, password }).then((r) => r.data)

/** Needs to be logged in. */
export const resendVerification = () =>
  client.post('/auth/resend-verification').then((r) => r.data)
