import { MantineProvider } from '@mantine/core'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { theme } from '../theme'
import { LoginPage } from './LoginPage'

function jsonResponse(status: number, body: unknown) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function renderLogin() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const router = createMemoryRouter(
    [
      { path: '/login', element: <LoginPage /> },
      { path: '/', element: <p>Home page</p> },
    ],
    { initialEntries: ['/login'] },
  )
  render(
    <MantineProvider theme={theme}>
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />
      </QueryClientProvider>
    </MantineProvider>,
  )
}

describe('LoginPage', () => {
  it('shows a friendly message for wrong credentials', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) =>
      String(input).endsWith('/auth/me')
        ? jsonResponse(401, { code: 'UNAUTHENTICATED' })
        : jsonResponse(401, {
            code: 'INVALID_CREDENTIALS',
            detail: 'Incorrect email or password.',
          }),
    )
    renderLogin()

    await userEvent.type(await screen.findByLabelText('Email'), 'someone@test.org')
    await userEvent.type(screen.getByLabelText('Password'), 'wrong')
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Incorrect email or password.')
  })

  it('goes to the home page after signing in', async () => {
    let signedIn = false
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      if (String(input).endsWith('/auth/login')) {
        signedIn = true
        return jsonResponse(200, {
          id: 1,
          email: 'admin@test.org',
          fullName: 'Admin',
          role: 'SUPER_ADMIN',
          departmentId: null,
          departmentName: null,
          mustChangePassword: false,
        })
      }
      return signedIn ? jsonResponse(500, {}) : jsonResponse(401, { code: 'UNAUTHENTICATED' })
    })
    renderLogin()

    await userEvent.type(await screen.findByLabelText('Email'), 'admin@test.org')
    await userEvent.type(screen.getByLabelText('Password'), 'Passw0rd123')
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByText('Home page')).toBeInTheDocument()
  })

  it('requires both fields before calling the server', async () => {
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValue(jsonResponse(401, { code: 'UNAUTHENTICATED' }))
    renderLogin()

    await userEvent.click(await screen.findByRole('button', { name: 'Sign in' }))

    expect(await screen.findAllByText('This field is required')).toHaveLength(2)
    expect(fetchMock).toHaveBeenCalledTimes(1) // only the initial "who am I" check
  })
})
