import { createTheme } from '@mantine/core'

// System fonts only: the app must work offline on a camp hotspot. Noto Sans Devanagari and
// Nirmala UI cover Hindi on Android and Windows.
export const theme = createTheme({
  primaryColor: 'teal',
  defaultRadius: 'md',
  fontFamily:
    'system-ui, -apple-system, "Segoe UI", Roboto, "Noto Sans", "Noto Sans Devanagari", "Nirmala UI", sans-serif',
  headings: { fontWeight: '650' },
  cursorType: 'pointer',
})
