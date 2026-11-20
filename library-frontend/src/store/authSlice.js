import { createAsyncThunk, createSlice } from '@reduxjs/toolkit'
import * as authApi from '../api/auth.js'
import { getProfile } from '../api/users.js'
import { clearAuth, loadAuth, saveAuth } from '../utils/storage.js'

/**
 * Who is logged in.
 *  - token + user start from localStorage, so a page reload keeps you logged in
 *  - the thunks below call the backend; pages dispatch them and read
 *    the result with .unwrap() (throws { message, status } on failure)
 */

// A thunk's error must be plain data, so keep only what pages need.
const toRejection = (e) => ({ message: e.message, status: e.status })

export const login = createAsyncThunk('auth/login', async ({ email, password }, { rejectWithValue }) => {
  try {
    return await authApi.login(email, password)
  } catch (e) {
    return rejectWithValue(toRejection(e))
  }
})

export const signup = createAsyncThunk('auth/signup', async (form, { rejectWithValue }) => {
  try {
    return await authApi.signup(form)
  } catch (e) {
    return rejectWithValue(toRejection(e))
  }
})

export const loginWithGoogle = createAsyncThunk('auth/google', async (credential, { rejectWithValue }) => {
  try {
    return await authApi.loginWithGoogle(credential)
  } catch (e) {
    return rejectWithValue(toRejection(e))
  }
})

/** Reloads the user (e.g. after verifying the email in another tab). */
export const refreshProfile = createAsyncThunk('auth/refreshProfile', async (_, { rejectWithValue }) => {
  try {
    return await getProfile()
  } catch (e) {
    return rejectWithValue(toRejection(e))
  }
})

const { token, user } = loadAuth()

// login, signup and Google all answer { jwt, user }: store them the same way.
const storeSession = (state, action) => {
  state.token = action.payload.jwt
  state.user = action.payload.user
  state.sessionExpired = false
  saveAuth(state.token, state.user)
}

const clearSession = (state) => {
  state.token = null
  state.user = null
  clearAuth()
}

const authSlice = createSlice({
  name: 'auth',
  // sessionExpired: the backend refused our token (401), so the login page can say why.
  initialState: { token, user, sessionExpired: false },
  reducers: {
    /** The user clicked "Log out". */
    loggedOut(state) {
      clearSession(state)
      state.sessionExpired = false
    },
    /** The token expired or became invalid (dispatched by the Axios client on a 401). */
    sessionExpired(state) {
      clearSession(state)
      state.sessionExpired = true
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(login.fulfilled, storeSession)
      .addCase(signup.fulfilled, storeSession)
      .addCase(loginWithGoogle.fulfilled, storeSession)
      .addCase(refreshProfile.fulfilled, (state, action) => {
        if (!state.token) return // logged out while the request was running
        state.user = action.payload
        saveAuth(state.token, state.user)
      })
  },
})

export const { loggedOut, sessionExpired } = authSlice.actions
export default authSlice.reducer

export const selectSessionExpired = (state) => state.auth.sessionExpired
export const selectToken = (state) => state.auth.token
export const selectUser = (state) => state.auth.user
export const selectIsLoggedIn = (state) => Boolean(state.auth.token)
export const selectIsAdmin = (state) => state.auth.user?.role === 'ROLE_ADMIN'
