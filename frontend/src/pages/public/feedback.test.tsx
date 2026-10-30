import { cleanup, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { PublicForm } from '../../api/types'
import i18n from '../../i18n'
import { jsonResponse, renderPage, signedInAs } from '../../test/renderPage'
import { EMPTY_ANSWERS, validateStep } from './feedbackDraft'
import { PublicCampPage } from './PublicCampPage'

const FORM: PublicForm = {
  name: 'Rampur Eye Camp',
  location: 'Government School, Rampur',
  startDate: '2026-11-02',
  endDate: '2026-11-03',
  feedbackState: 'OPEN',
  services: [{ id: 11, name: 'Cataract screening', departmentName: 'Eye Care' }],
  categories: [
    { code: 'WAITING_TIME', labelEn: 'Waiting time', labelHi: 'इंतज़ार का समय', required: false },
    { code: 'OVERALL', labelEn: 'Overall experience', labelHi: 'कुल मिलाकर अनुभव', required: true },
  ],
}

function renderPublic(path = '/f/dKGwhkcu') {
  renderPage(signedInAs('ANALYST'), [{ path: '/f/:token', element: <PublicCampPage /> }], path)
}

/** Answers the form endpoints like the server; returns the submitted bodies. */
function mockServer(form: PublicForm = FORM) {
  const submitted: unknown[] = []
  const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
    const url = String(input)
    if (url.endsWith('/form-token')) return jsonResponse(200, { submissionToken: 'tok-1' })
    if (url.endsWith('/feedback')) {
      submitted.push(JSON.parse(String(init?.body)))
      return jsonResponse(201, { submittedAt: '2026-11-02T05:00:00Z' })
    }
    return jsonResponse(200, form)
  })
  return { fetchMock, submitted }
}

beforeEach(() => {
  localStorage.clear()
})

afterEach(async () => {
  await i18n.changeLanguage('en')
})

describe('validateStep', () => {
  it('needs a service choice and the required ratings', () => {
    expect(validateStep('service', EMPTY_ANSWERS, ['OVERALL'])).toEqual({
      service: 'feedbackForm.service.required',
    })
    expect(
      validateStep('ratings', { ...EMPTY_ANSWERS, ratings: { WAITING_TIME: 2 } }, ['OVERALL']),
    ).toEqual({ OVERALL: 'feedbackForm.ratings.required' })
  })

  it('asks for a way to reach the attendee, and consent, only when they want a reply', () => {
    expect(validateStep('followUp', EMPTY_ANSWERS, [])).toEqual({})
    const wantsReply = { ...EMPTY_ANSWERS, wantsFollowUp: true }
    expect(validateStep('followUp', wantsReply, [])).toEqual({
      contact: 'feedbackForm.followUp.contactRequired',
      consent: 'feedbackForm.followUp.consentRequired',
    })
    expect(
      validateStep(
        'followUp',
        { ...wantsReply, contactPhone: '12-34', contactEmail: 'nope', consent: true },
        [],
      ),
    ).toEqual({
      contactPhone: 'feedbackForm.followUp.invalidPhone',
      contactEmail: 'feedbackForm.followUp.invalidEmail',
    })
    expect(
      validateStep(
        'followUp',
        { ...wantsReply, contactPhone: '+91 98765 43210', consent: true },
        [],
      ),
    ).toEqual({})
  })
})

describe('PublicCampPage', () => {
  it('lets an attendee give feedback in Hindi from start to finish, without signing in', async () => {
    const { fetchMock, submitted } = mockServer()
    renderPublic()

    await userEvent.click(await screen.findByRole('button', { name: 'हिन्दी' }))
    await userEvent.click(await screen.findByRole('radio', { name: /Cataract screening/ }))
    await userEvent.click(screen.getByRole('button', { name: 'आगे' }))

    const overall = screen.getByRole('group', { name: /कुल मिलाकर अनुभव/ })
    await userEvent.click(within(overall).getByRole('radio', { name: /बहुत अच्छा/ }))
    await userEvent.click(screen.getByRole('button', { name: 'आगे' }))

    await userEvent.type(screen.getByRole('textbox'), 'साफ़-सफ़ाई अच्छी थी')
    await userEvent.click(screen.getByRole('button', { name: 'आगे' }))
    await userEvent.click(screen.getByRole('button', { name: 'फ़ीडबैक भेजें' }))

    expect(await screen.findByText('धन्यवाद!')).toBeInTheDocument()
    expect(submitted).toEqual([
      {
        language: 'hi',
        serviceId: 11,
        ratings: { OVERALL: 5 },
        comment: 'साफ़-सफ़ाई अच्छी थी',
        problemReported: false,
        followUp: null,
        assisted: false,
        submissionToken: 'tok-1',
      },
    ])
    // Attendees never trigger the staff "who am I" call.
    expect(fetchMock.mock.calls.some(([url]) => String(url).includes('/auth/me'))).toBe(false)
  })

  it('does not move on until the required rating is chosen', async () => {
    mockServer()
    renderPublic()

    await userEvent.click(await screen.findByRole('button', { name: 'English' }))
    await userEvent.click(await screen.findByRole('radio', { name: /General visit/ }))
    await userEvent.click(screen.getByRole('button', { name: 'Next' }))
    await userEvent.click(screen.getByRole('button', { name: 'Next' }))

    expect(screen.getByText('Please choose a face')).toBeInTheDocument()
    expect(screen.getByText('How was your visit?')).toBeInTheDocument()
  })

  it('keeps unsent answers on the phone after a reload', async () => {
    mockServer()
    renderPublic()
    await userEvent.click(await screen.findByRole('button', { name: 'English' }))
    await userEvent.click(await screen.findByRole('radio', { name: /Cataract screening/ }))
    await userEvent.click(screen.getByRole('button', { name: 'Next' }))

    cleanup()
    renderPublic()

    expect(await screen.findByText('How was your visit?')).toBeInTheDocument()
  })

  it('gently warns when this phone already sent feedback for the camp', async () => {
    localStorage.setItem('medcamp.feedbackSent.dKGwhkcu', '2026-11-02T05:00:00Z')
    mockServer()
    renderPublic()

    expect(await screen.findByText('You have already given feedback')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Yes, give feedback' }))
    expect(await screen.findByRole('button', { name: 'English' })).toBeInTheDocument()
  })

  it('shows a closed camp without the form', async () => {
    mockServer({ ...FORM, feedbackState: 'ENDED' })
    renderPublic()

    expect(await screen.findByText('Feedback has closed')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'English' })).not.toBeInTheDocument()
  })

  it('explains an invalid link', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      jsonResponse(404, { code: 'CAMP_LINK_INVALID', detail: 'This feedback link is not valid.' }),
    )
    renderPublic('/f/nope1234')

    expect(await screen.findByText('This link is not valid')).toBeInTheDocument()
  })

  it('points to the camp Wi-Fi when the server cannot be reached', async () => {
    vi.spyOn(globalThis, 'fetch').mockRejectedValue(new TypeError('Failed to fetch'))
    renderPublic()

    expect(await screen.findByText('Cannot reach the camp')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Try again' })).toBeInTheDocument()
  })

  it('marks forms filled in by signed-in staff as assisted', async () => {
    const { submitted } = mockServer()
    renderPage(
      { ...signedInAs('MEDICAL_STAFF'), fullName: 'Volunteer Kiran' },
      [{ path: '/f/:token', element: <PublicCampPage /> }],
      '/f/dKGwhkcu?assisted=1',
    )

    expect(await screen.findByText(/Volunteer Kiran is filling in this form/)).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'English' }))
    await userEvent.click(await screen.findByRole('radio', { name: /General visit/ }))
    await userEvent.click(screen.getByRole('button', { name: 'Next' }))
    const overall = screen.getByRole('group', { name: /Overall experience/ })
    await userEvent.click(within(overall).getByRole('radio', { name: /Okay/ }))
    await userEvent.click(screen.getByRole('button', { name: 'Next' }))
    await userEvent.click(screen.getByRole('button', { name: 'Next' }))
    await userEvent.click(screen.getByRole('button', { name: 'Send feedback' }))

    expect(await screen.findByRole('button', { name: 'Next attendee' })).toBeInTheDocument()
    expect(submitted).toMatchObject([{ assisted: true, serviceId: null, ratings: { OVERALL: 3 } }])
  })
})
