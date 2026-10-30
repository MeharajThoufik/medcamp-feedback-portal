import { MantineProvider } from '@mantine/core'
import { DatesProvider } from '@mantine/dates'
import { ModalsProvider } from '@mantine/modals'
import { Notifications } from '@mantine/notifications'
import { QueryClientProvider } from '@tanstack/react-query'
import 'dayjs/locale/hi'
import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { RouterProvider } from 'react-router'
import { queryClient } from './api/queryClient'
import { router } from './router'
import { theme } from './theme'

/** Date pickers follow the chosen language; weeks start on Monday as on Indian calendars. */
function LocalizedDates({ children }: { children: ReactNode }) {
  const { i18n } = useTranslation()
  return (
    <DatesProvider settings={{ locale: i18n.language, firstDayOfWeek: 1 }}>
      {children}
    </DatesProvider>
  )
}

export function App() {
  return (
    <MantineProvider theme={theme} defaultColorScheme="auto">
      <QueryClientProvider client={queryClient}>
        <LocalizedDates>
          <ModalsProvider>
            <Notifications position="top-right" />
            <RouterProvider router={router} />
          </ModalsProvider>
        </LocalizedDates>
      </QueryClientProvider>
    </MantineProvider>
  )
}
