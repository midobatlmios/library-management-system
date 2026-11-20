import { IconButton, Tooltip } from '@mui/material'
import { useColorScheme } from '@mui/material/styles'
import DarkModeIcon from '@mui/icons-material/DarkMode'
import LightModeIcon from '@mui/icons-material/LightMode'

/** Switches between light and dark. MUI remembers the choice in localStorage. */
export default function ThemeToggle() {
  const { mode, systemMode, setMode } = useColorScheme()
  if (!mode) return null // first render, before MUI knows the mode

  const current = mode === 'system' ? systemMode : mode
  const next = current === 'dark' ? 'light' : 'dark'

  return (
    <Tooltip title={`Switch to ${next} mode`}>
      <IconButton color="inherit" onClick={() => setMode(next)} aria-label={`Switch to ${next} mode`}>
        {current === 'dark' ? <LightModeIcon /> : <DarkModeIcon />}
      </IconButton>
    </Tooltip>
  )
}
