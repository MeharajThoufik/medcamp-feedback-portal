import { render, screen } from '@testing-library/react'
import { MantineProvider } from '@mantine/core'
import { describe, expect, it, vi } from 'vitest'
import type { Camp, Poster } from '../../api/types'
import { jsonResponse, renderPage, signedInAs } from '../../test/renderPage'
import { theme } from '../../theme'
import { PublicCampPage } from '../public/PublicCampPage'
import { PosterSheet } from './CampPosterPage'
import { CampQrTab } from './CampQrTab'
import { isLocalOnlyLink } from './links'

const CAMP: Camp = {
  id: 3,
  name: 'Rampur Eye Camp',
  location: 'Government School, Rampur',
  startDate: '2026-11-02',
  endDate: '2026-11-03',
  status: 'UPCOMING',
  feedbackState: 'CAMP_NOT_ACTIVE',
  feedbackOpensOn: '2026-11-02',
  feedbackClosesOn: '2026-11-10',
  publicToken: 'dKGwhkcu',
  feedbackUrl: 'http://192.168.137.1:8080/f/dKGwhkcu',
  organizerId: 7,
  organizerName: 'Asha Rao',
  services: [],
  canEdit: true,
  nextStatuses: ['ACTIVE', 'ARCHIVED'],
  createdAt: '2026-10-29T09:00:00Z',
  updatedAt: '2026-10-29T09:00:00Z',
}

const POSTER: Poster = {
  campId: 3,
  name: 'Rampur Eye Camp',
  location: 'Government School, Rampur',
  startDate: '2026-11-02',
  endDate: '2026-11-03',
  feedbackUrl: 'http://192.168.137.1:8080/f/dKGwhkcu',
  wifi: null,
}

function renderQrTab(camp: Camp) {
  renderPage(signedInAs('ORGANIZER'), [{ path: '/', element: <CampQrTab camp={camp} /> }], '/')
}

describe('isLocalOnlyLink', () => {
  it('spots links that only work on the laptop itself', () => {
    expect(isLocalOnlyLink('http://localhost:8080/f/abc')).toBe(true)
    expect(isLocalOnlyLink('http://127.0.0.1:8080/f/abc')).toBe(true)
    expect(isLocalOnlyLink('http://192.168.137.1:8080/f/abc')).toBe(false)
    expect(isLocalOnlyLink('https://camp.example.org/f/abc')).toBe(false)
  })
})

describe('CampQrTab', () => {
  it('shows the link, the QR code and the poster button', () => {
    renderQrTab(CAMP)

    expect(screen.getByText('http://192.168.137.1:8080/f/dKGwhkcu')).toBeInTheDocument()
    expect(screen.getByRole('img', { name: 'QR code for Rampur Eye Camp' })).toHaveAttribute(
      'src',
      expect.stringContaining('/api/camps/3/qr.png'),
    )
    expect(screen.getByRole('link', { name: 'Print poster' })).toHaveAttribute(
      'href',
      '/camps/3/poster',
    )
    expect(screen.getByRole('button', { name: 'Make a new link' })).toBeInTheDocument()
    expect(screen.queryByText('Phones cannot open this link yet')).not.toBeInTheDocument()
  })

  it('warns when phones cannot reach the link', () => {
    renderQrTab({ ...CAMP, feedbackUrl: 'http://localhost:8080/f/dKGwhkcu' })

    expect(screen.getByText('Phones cannot open this link yet')).toBeInTheDocument()
  })

  it('offers only viewing to people who cannot change the camp', () => {
    renderQrTab({ ...CAMP, canEdit: false, nextStatuses: [] })

    expect(screen.queryByRole('link', { name: 'Print poster' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Make a new link' })).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Download QR image' })).toBeInTheDocument()
  })
})

describe('PosterSheet', () => {
  function renderPoster(poster: Poster) {
    render(
      <MantineProvider theme={theme}>
        <PosterSheet poster={poster} />
      </MantineProvider>,
    )
  }

  it('prints the instructions in English and Hindi, and the short link', () => {
    renderPoster(POSTER)

    expect(screen.getByText('Tell us about your visit')).toBeInTheDocument()
    expect(screen.getByText('अपने अनुभव के बारे में बताएँ')).toBeInTheDocument()
    expect(screen.getByText('192.168.137.1:8080/f/dKGwhkcu')).toBeInTheDocument()
    expect(screen.queryByText('Join the camp Wi-Fi')).not.toBeInTheDocument()
  })

  it('adds a Wi-Fi step when the camp Wi-Fi is set', () => {
    renderPoster({ ...POSTER, wifi: { ssid: 'CampWiFi', password: 'secret123' } })

    expect(screen.getByText('Join the camp Wi-Fi')).toBeInTheDocument()
    expect(screen.getByText('CampWiFi')).toBeInTheDocument()
    expect(screen.getByText('secret123')).toBeInTheDocument()
    expect(screen.getByRole('img', { name: 'Join the camp Wi-Fi' })).toHaveAttribute(
      'src',
      expect.stringContaining('/api/settings/wifi/qr.png'),
    )
  })
})

describe('PublicCampPage', () => {
  function renderPublic(token: string) {
    renderPage(
      signedInAs('ANALYST'),
      [{ path: '/f/:token', element: <PublicCampPage /> }],
      `/f/${token}`,
    )
  }

  it('confirms the link works for an open camp, without asking anyone to sign in', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      jsonResponse(200, {
        name: 'Rampur Eye Camp',
        location: 'Government School, Rampur',
        startDate: '2026-11-02',
        endDate: '2026-11-03',
        feedbackState: 'OPEN',
      }),
    )
    renderPublic('dKGwhkcu')

    expect(await screen.findByText('Rampur Eye Camp')).toBeInTheDocument()
    expect(screen.getByText('Thank you for scanning!')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(String(fetchMock.mock.calls[0][0])).toBe('/api/public/camps/dKGwhkcu')
  })

  it('explains an invalid link', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      jsonResponse(404, { code: 'CAMP_LINK_INVALID', detail: 'This feedback link is not valid.' }),
    )
    renderPublic('nope1234')

    expect(await screen.findByText('This link is not valid')).toBeInTheDocument()
  })

  it('points to the camp Wi-Fi when the server cannot be reached', async () => {
    vi.spyOn(globalThis, 'fetch').mockRejectedValue(new TypeError('Failed to fetch'))
    renderPublic('dKGwhkcu')

    expect(await screen.findByText('Cannot reach the camp')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Try again' })).toBeInTheDocument()
  })
})
