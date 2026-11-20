import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { Provider } from 'react-redux'
import { BrowserRouter } from 'react-router-dom'
import { CssBaseline, StyledEngineProvider, ThemeProvider } from '@mui/material'
import './index.css'
import App from './App.jsx'
import { ToastProvider } from './components/feedback/ToastProvider.jsx'
import theme from './theme.js'
import { store } from './store/index.js'
import { injectStore } from './api/client.js'

// The Axios instance needs the store (to read the token and to log out on 401).
injectStore(store)

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <Provider store={store}>
      <StyledEngineProvider enableCssLayer>
        <ThemeProvider theme={theme} defaultMode="system">
          <CssBaseline />
          <BrowserRouter>
            <ToastProvider>
              <App />
            </ToastProvider>
          </BrowserRouter>
        </ThemeProvider>
      </StyledEngineProvider>
    </Provider>
  </StrictMode>,
)
