/** The same password rules as the backend (UserDTO / ResetPasswordRequest). */
export const PASSWORD_RULES = [
  { label: '8 to 72 characters', test: (p) => p.length >= 8 && p.length <= 72 },
  { label: 'At least one letter', test: (p) => /[A-Za-z]/.test(p) },
  { label: 'At least one digit', test: (p) => /\d/.test(p) },
]

export const isPasswordValid = (p) => PASSWORD_RULES.every((r) => r.test(p))
