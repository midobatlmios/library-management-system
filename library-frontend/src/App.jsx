import { useEffect } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import AppRoutes from './routes/AppRoutes.jsx'
import { refreshProfile, selectToken } from './store/authSlice.js'

export default function App() {
  const dispatch = useDispatch()
  const token = useSelector(selectToken)

  // On load (and after each login), reload the user from the backend so role
  // and verified are current. An expired token answers 401, which logs out.
  useEffect(() => {
    if (token) dispatch(refreshProfile())
  }, [token, dispatch])

  return <AppRoutes />
}
