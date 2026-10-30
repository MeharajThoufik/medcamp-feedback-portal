import { screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import type { Camp, FeedbackItem, FeedbackSummary } from '../../api/types'
import { jsonResponse, renderPage, signedInAs } from '../../test/renderPage'
import { CampFeedbackTab } from './CampFeedbackTab'

const CAMP = {
  id: 3,
  name: 'Rampur Eye Camp',
  publicToken: 'dKGwhkcu',
  feedbackState: 'OPEN',
} as Camp

const SUMMARY: FeedbackSummary = {
  total: 1,
  categories: [
    {
      code: 'WAITING_TIME',
      labelEn: 'Waiting time',
      labelHi: 'इंतज़ार का समय',
      average: 2,
      count: 1,
    },
    { code: 'OVERALL', labelEn: 'Overall experience', labelHi: 'कुल', average: 4, count: 1 },
  ],
}

const ITEM: FeedbackItem = {
  id: 1,
  submittedAt: '2026-11-02T05:00:00Z',
  serviceId: 11,
  serviceName: 'Cataract screening',
  departmentName: 'Eye Care',
  ratings: { WAITING_TIME: 2, OVERALL: 4 },
  comment: 'Long queue',
  problemReported: true,
  complaint: {
    referenceNo: 'MC-26-K7Q2XP',
    status: 'OPEN',
    priority: 'MEDIUM',
    categoryCode: 'WAITING',
    categoryLabelEn: 'Long wait or crowding',
    categoryLabelHi: 'लंबा इंतज़ार या भीड़',
    description: 'Waited two hours at registration',
  },
  language: 'hi',
  anonymous: false,
  assisted: false,
  assistedByName: null,
  contact: { name: 'Meena', phone: '+919876543210', email: null },
}

function renderTab(role: 'ORGANIZER' | 'ANALYST') {
  renderPage(signedInAs(role), [{ path: '/', element: <CampFeedbackTab camp={CAMP} /> }], '/')
}

describe('CampFeedbackTab', () => {
  it('shows the summary and each response, with consented contact details', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) =>
      String(input).includes('/summary')
        ? jsonResponse(200, SUMMARY)
        : jsonResponse(200, { items: [ITEM], page: 0, size: 20, totalItems: 1, totalPages: 1 }),
    )
    renderTab('ORGANIZER')

    expect(await screen.findByText('1 response')).toBeInTheDocument()
    expect(screen.getByText('Long queue')).toBeInTheDocument()
    expect(screen.getByText('Waiting time: 2/5')).toBeInTheDocument()
    expect(screen.getByText('Long wait or crowding')).toBeInTheDocument()
    expect(screen.getByText('MC-26-K7Q2XP')).toBeInTheDocument()
    expect(screen.getByText('Waited two hours at registration')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '+919876543210' })).toHaveAttribute(
      'href',
      'tel:+919876543210',
    )
    expect(screen.getByRole('link', { name: 'Fill in for an attendee' })).toHaveAttribute(
      'href',
      '/f/dKGwhkcu?assisted=1',
    )
  })

  it('tells analysts where to find combined results, without loading any feedback', () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch')
    renderTab('ANALYST')

    expect(screen.getByText(/Analysts see combined, anonymous results/)).toBeInTheDocument()
    expect(fetchMock).not.toHaveBeenCalled()
  })
})
