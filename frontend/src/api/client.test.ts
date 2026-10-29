import { describe, expect, it, vi } from 'vitest'
import { api, ApiError } from './client'

function jsonResponse(status: number, body: unknown) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('api client', () => {
  it('sends the CSRF cookie back as a header on writes', async () => {
    document.cookie = 'XSRF-TOKEN=abc123'
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValue(new Response(null, { status: 204 }))

    await api('/departments', { method: 'POST', json: { name: 'Dental' } })

    const [url, init] = fetchMock.mock.calls[0]!
    expect(url).toBe('/api/departments')
    expect((init!.headers as Record<string, string>)['X-XSRF-TOKEN']).toBe('abc123')
    expect(init!.body).toBe('{"name":"Dental"}')
  })

  it('turns problem+json into an ApiError with code and field errors', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      jsonResponse(400, {
        status: 400,
        code: 'VALIDATION_FAILED',
        detail: 'Some fields are invalid.',
        errors: { email: 'must be a well-formed email address' },
      }),
    )

    const error = await api('/users', { method: 'POST', json: {} }).catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({ status: 400, code: 'VALIDATION_FAILED' })
    expect((error as ApiError).fieldErrors.email).toBeDefined()
  })

  it('refreshes the CSRF token and retries once when it was stale', async () => {
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(jsonResponse(403, { code: 'CSRF_INVALID' }))
      .mockResolvedValueOnce(jsonResponse(401, { code: 'UNAUTHENTICATED' }))
      .mockResolvedValueOnce(jsonResponse(201, { id: 1 }))

    const result = await api<{ id: number }>('/departments', {
      method: 'POST',
      json: { name: 'X' },
    })

    expect(result.id).toBe(1)
    expect(fetchMock).toHaveBeenCalledTimes(3)
    expect(fetchMock.mock.calls[1]![0]).toBe('/api/auth/me')
  })

  it('reports an unreachable server as NETWORK_ERROR', async () => {
    vi.spyOn(globalThis, 'fetch').mockRejectedValue(new TypeError('Failed to fetch'))

    await expect(api('/auth/me')).rejects.toMatchObject({ code: 'NETWORK_ERROR', status: 0 })
  })
})
