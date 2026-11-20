import { createTheme } from '@mui/material/styles'

/**
 * Enamel Street Plaques: cobalt enamel, enamel white, steel and ink.
 * Light mode: a pale steel ground with white plates (a street in daylight).
 * Dark mode: a night-blue ground, plates keep their enamel colours.
 * colorSchemeSelector 'class' puts class="light" or "dark" on <html>, which
 * also switches the enamel tokens in index.css and Tailwind's dark: variant.
 */
const SIGN_FONT = '"Overpass Variable", system-ui, "Segoe UI", Roboto, Helvetica, Arial, sans-serif'

const theme = createTheme({
  cssVariables: { colorSchemeSelector: 'class' },
  colorSchemes: {
    light: {
      palette: {
        primary: { main: '#1b3c9e', dark: '#132c76', light: '#3a5fc8', contrastText: '#ffffff' },
        secondary: { main: '#b67d06', contrastText: '#12151c' },
        success: { main: '#1d7a4b' },
        error: { main: '#a8261f' },
        warning: { main: '#b67d06' },
        info: { main: '#0d6672' },
        background: { default: '#e8ebf1', paper: '#f6f7f3' },
        text: { primary: '#12151c', secondary: '#4a5163' },
        divider: 'rgba(27, 60, 158, 0.16)',
      },
    },
    dark: {
      palette: {
        primary: { main: '#7f9cff', dark: '#5b7cf0', light: '#a8bbff', contrastText: '#0d1120' },
        secondary: { main: '#e2a72e', contrastText: '#12151c' },
        success: { main: '#4cc38a' },
        error: { main: '#f07a6f' },
        warning: { main: '#e2a72e' },
        info: { main: '#4fb6c2' },
        background: { default: '#0d1120', paper: '#161c2e' },
        text: { primary: '#eef1f7', secondary: '#a7afc2' },
        divider: 'rgba(127, 156, 255, 0.18)',
      },
    },
  },
  shape: { borderRadius: 10 },
  typography: {
    fontFamily: SIGN_FONT,
    h1: { fontWeight: 900, letterSpacing: '-0.01em' },
    h2: { fontWeight: 900, letterSpacing: '-0.01em' },
    h3: { fontWeight: 900, letterSpacing: '-0.01em' },
    h4: { fontWeight: 800, letterSpacing: '-0.005em' },
    h5: { fontWeight: 800 },
    h6: { fontWeight: 800 },
    button: { textTransform: 'none', fontWeight: 800, letterSpacing: '0.01em' },
    overline: { fontWeight: 800, letterSpacing: '0.12em' },
  },
  components: {
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: {
        root: { borderRadius: 9 },
        // Contained buttons are small enamel plates with an inset rim.
        contained: {
          outline: '1.5px solid rgba(255,255,255,0.85)',
          outlineOffset: -4,
          paddingInline: 18,
        },
        outlined: { borderWidth: 2, '&:hover': { borderWidth: 2 } },
      },
    },
    MuiCard: {
      defaultProps: { variant: 'outlined' },
      styleOverrides: {
        // Cards are white enamel plates: a thin rim set in from the edge and the plate's soft drop.
        root: ({ theme }) => ({
          borderRadius: 14,
          borderColor: theme.vars.palette.divider,
          outline: `1px solid ${theme.vars.palette.divider}`,
          outlineOffset: -6,
          boxShadow: 'var(--plate-shadow)',
        }),
      },
    },
    MuiPaper: {
      styleOverrides: { rounded: { borderRadius: 14 } },
    },
    MuiChip: {
      styleOverrides: { root: { fontWeight: 700, borderRadius: 7 } },
    },
    MuiAppBar: {
      styleOverrides: {
        root: {
          backgroundColor: 'var(--enamel-cobalt)',
          color: '#fff',
          backgroundImage: 'none',
          // The bar is the top edge of a street sign: a white rim line under it.
          boxShadow: 'inset 0 -3px 0 var(--enamel-cobalt), inset 0 -5px 0 rgba(255,255,255,0.9), 0 6px 18px -10px rgba(10,18,40,0.6)',
        },
      },
    },
    MuiOutlinedInput: {
      styleOverrides: { root: { borderRadius: 9 } },
    },
    // A selected item in a side list (genres, admin menu) is struck forward as a cobalt plate.
    MuiListItemButton: {
      styleOverrides: {
        root: {
          borderRadius: 8,
          '&.Mui-selected, &.Mui-selected:hover': {
            backgroundColor: 'var(--enamel-cobalt)',
            color: '#fff',
            outline: '1.5px solid rgba(255,255,255,0.85)',
            outlineOffset: -4,
          },
          '&.Mui-selected .MuiListItemIcon-root, &.Mui-selected .MuiTypography-root': { color: '#fff' },
        },
      },
    },
    MuiTab: {
      styleOverrides: { root: { fontWeight: 800, textTransform: 'none' } },
    },
    // The tab under the current page is underlined by a thick enamel bar.
    MuiTabs: {
      styleOverrides: { indicator: { height: 4, borderRadius: '4px 4px 0 0' } },
    },
    // Admin tables and other papers keep the plate's corner and shadow.
    MuiTableCell: {
      styleOverrides: { head: { fontWeight: 800 } },
    },
    MuiTooltip: {
      styleOverrides: { tooltip: { fontWeight: 600 } },
    },
  },
})

export default theme
