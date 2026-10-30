import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { CampSummary, Page } from '../../api/types'
import { jsonResponse, renderPage, signedInAs } from '../../test/renderPage'
import { defaultFeedbackWindow } from './campDates'
import { NewCampPage } from './CampFormPage'
import { CampsPage } from './CampsPage'
import { statusActions } from './statusActions'

const RAMPUR: CampSummary = {
  id: 3,
  name: 'Rampur Eye Camp',
  location: 'Government School, Rampur',
  startDate: '2026-11-02',
  endDate: '2026-11-03',
  status: 'ACTIVE',
  feedbackState: 'OPEN',
  organizerId: 7,
  organizerName: 'Asha Rao',
  serviceCount: 2,
}

function page<T>(items: T[]): Page<T> {
  return { items, page: 0, size: 20, totalItems: items.length, totalPages: 1 }
}

describe('camp helpers', () => {
  it('opens feedback on the first camp day and closes it a week after the last', () => {
    expect(defaultFeedbackWindow('2026-10-30', '2026-10-31')).toEqual(['2026-10-30', '2026-11-07'])
  })

  it('offers the main next step first', () => {
    expect(statusActions('UPCOMING', ['ACTIVE', 'ARCHIVED']).map((a) => a.key)).toEqual([
      'start',
      'archive',
    ])
    expect(statusActions('CLOSED', ['ACTIVE', 'ARCHIVED']).map((a) => a.key)).toEqual([
      'reopen',
      'archive',
    ])
    expect(statusActions('ARCHIVED', ['CLOSED'])).toMatchObject([
      { key: 'restore', confirm: false },
    ])
    expect(statusActions('ACTIVE', ['CLOSED'])).toMatchObject([{ key: 'close', confirm: true }])
  })
})

describe('CampsPage', () => {
  it('lists camps and lets an organizer create one', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(jsonResponse(200, page([RAMPUR])))
    renderPage(signedInAs('ORGANIZER'), [{ path: '/camps', element: <CampsPage /> }], '/camps')

    expect(await screen.findByRole('link', { name: 'Rampur Eye Camp' })).toBeInTheDocument()
    expect(screen.getByText('Feedback open')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'New camp' })).toBeInTheDocument()
    // Organizers only see their own camps, so there is no organizer column.
    expect(screen.queryByRole('columnheader', { name: 'Organizer' })).not.toBeInTheDocument()
  })

  it('shows analysts every camp without a create button', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(jsonResponse(200, page([RAMPUR])))
    renderPage(signedInAs('ANALYST'), [{ path: '/camps', element: <CampsPage /> }], '/camps')

    expect(await screen.findByRole('link', { name: 'Rampur Eye Camp' })).toBeInTheDocument()
    expect(screen.getByRole('columnheader', { name: 'Organizer' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'New camp' })).not.toBeInTheDocument()
  })
})

describe('NewCampPage', () => {
  it('checks required fields before calling the server', async () => {
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValue(jsonResponse(200, [{ id: 1, name: 'Eye Care', active: true }]))
    renderPage(
      signedInAs('ORGANIZER'),
      [{ path: '/camps/new', element: <NewCampPage /> }],
      '/camps/new',
    )

    await userEvent.click(await screen.findByRole('button', { name: 'Create camp' }))

    expect(await screen.findAllByText('This field is required')).toHaveLength(2)
    expect(screen.getByText('Pick the first and last day of the camp')).toBeInTheDocument()
    expect(screen.getByText('Pick when feedback opens and closes')).toBeInTheDocument()
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'POST')).toBe(false)
    // Organizers always run their own camps, so they are not asked to pick one.
    expect(screen.queryByText('Organizer')).not.toBeInTheDocument()
  })

  it('flags two services with the same name', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      jsonResponse(200, [{ id: 1, name: 'Eye Care', active: true }]),
    )
    renderPage(
      signedInAs('ORGANIZER'),
      [{ path: '/camps/new', element: <NewCampPage /> }],
      '/camps/new',
    )

    await userEvent.click(await screen.findByRole('button', { name: 'Add service' }))
    await userEvent.click(screen.getByRole('button', { name: 'Add service' }))
    const names = screen.getAllByRole('textbox', { name: 'Service name' })
    await userEvent.type(names[0], 'Eye test')
    await userEvent.type(names[1], 'eye TEST')
    await userEvent.click(screen.getByRole('button', { name: 'Create camp' }))

    expect(await screen.findByText('This service is already listed')).toBeInTheDocument()
  })
})
