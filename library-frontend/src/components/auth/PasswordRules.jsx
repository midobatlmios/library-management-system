import { Stack, Typography } from '@mui/material'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import RadioButtonUncheckedIcon from '@mui/icons-material/RadioButtonUnchecked'
import { PASSWORD_RULES } from '../../utils/password.js'

/** Live checklist under a new-password field. */
export default function PasswordRules({ password }) {
  return (
    <Stack spacing={0.5} component="ul" sx={{ m: 0, p: 0, listStyle: 'none' }}>
      {PASSWORD_RULES.map((rule) => {
        const ok = rule.test(password)
        return (
          <Stack
            key={rule.label}
            component="li"
            direction="row"
            spacing={1}
            sx={{ alignItems: 'center', color: ok ? 'success.main' : 'text.secondary' }}
          >
            {ok ? <CheckCircleIcon fontSize="small" /> : <RadioButtonUncheckedIcon fontSize="small" />}
            <Typography variant="body2">{rule.label}</Typography>
          </Stack>
        )
      })}
    </Stack>
  )
}
