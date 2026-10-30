import { MantineProvider } from '@mantine/core'
import { DatesProvider } from '@mantine/dates'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import type { ReactNode } from 'react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { meKey } from '../api/queryClient'
import type { Me, Role } from '../api/types'
import { theme } from '../theme'

export function jsonResponse(status: number, body: unknown) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

export function signedInAs(role: Role): Me {
  return {
    id: 7,
    email: 'someone@test.org',
    fullName: 'Asha Rao',
    role,
    departmentId: null,
    departmentName: null,
    mustChangePassword: false,
  }
}

/** Renders one page at `path` for an already signed-in user. */
export function renderPage(
  me: Me,
  routes: { path: string; element: ReactNode }[],
  initialEntry: string,
) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  queryClient.setQueryData(meKey, me)
  const router = createMemoryRouter(routes, { initialEntries: [initialEntry] })
  render(
    <MantineProvider theme={theme}>
      <DatesProvider settings={{ locale: 'en' }}>
        <QueryClientProvider client={queryClient}>
          <RouterProvider router={router} />
        </QueryClientProvider>
      </DatesProvider>
    </MantineProvider>,
  )
  return { router }
}
