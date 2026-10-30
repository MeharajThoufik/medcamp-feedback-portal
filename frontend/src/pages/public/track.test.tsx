import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { jsonResponse, renderPage, signedInAs } from '../../test/renderPage'
import { TrackComplaintPage } from './TrackComplaintPage'

function renderTrack(path = '/track') {
  renderPage(signedInAs('ANALYST'), [{ path: '/track', element: <TrackComplaintPage /> }], path)
}

describe('TrackComplaintPage', () => {
  it('shows how far a complaint has got, with the team message', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      jsonResponse(200, {
        referenceNo: 'MC-26-K7Q2XP',
        status: 'IN_PROGRESS',
        submittedAt: '2026-11-02T05:00:00Z',
        lastUpdatedAt: '2026-11-02T08:00:00Z',
        publicNote: 'Cleaners have been sent to gate 2.',
      }),
    )
    renderTrack()

    await userEvent.type(screen.getByRole('textbox', { name: 'Reference number' }), 'mc 26 k7q2xp')
    await userEvent.click(screen.getByRole('button', { name: 'Check' }))

    expect(await screen.findByText('Being looked into')).toBeInTheDocument()
    expect(screen.getByText('Cleaners have been sent to gate 2.')).toBeInTheDocument()
    expect(String(fetchMock.mock.calls[0][0])).toBe('/api/public/complaints/mc%2026%20k7q2xp')
  })

  it('looks up the reference from the link on the thank-you screen straight away', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      jsonResponse(200, {
        referenceNo: 'MC-26-K7Q2XP',
        status: 'OPEN',
        submittedAt: '2026-11-02T05:00:00Z',
        lastUpdatedAt: '2026-11-02T05:00:00Z',
        publicNote: null,
      }),
    )
    renderTrack('/track?ref=MC-26-K7Q2XP')

    expect(await screen.findByText('MC-26-K7Q2XP')).toBeInTheDocument()
    expect(screen.getByRole('textbox', { name: 'Reference number' })).toHaveValue('MC-26-K7Q2XP')
  })

  it('says only "not found" for a wrong reference', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      jsonResponse(404, {
        code: 'COMPLAINT_NOT_FOUND',
        detail: 'No complaint has this reference.',
      }),
    )
    renderTrack('/track?ref=MC-26-AAAAAA')

    expect(await screen.findByText('No complaint found')).toBeInTheDocument()
  })

  it('does not call the server without a reference', () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch')
    renderTrack()

    expect(screen.getByRole('heading', { name: 'Check my complaint' })).toBeInTheDocument()
    expect(fetchMock).not.toHaveBeenCalled()
  })
})
