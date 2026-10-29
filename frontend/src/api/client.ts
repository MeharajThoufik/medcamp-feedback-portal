/** Error returned by the backend as problem+json; `code` is stable and safe to switch on. */
export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly fieldErrors: Record<string, string>

  constructor(status: number, code: string, message: string, fieldErrors?: Record<string, string>) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.fieldErrors = fieldErrors ?? {}
  }
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  /** Sent as a JSON body. */
  json?: unknown
  /** Sent as a form body (used by sign-in). */
  form?: Record<string, string>
  signal?: AbortSignal
}

function readCookie(name: string): string | undefined {
  const match = document.cookie.split('; ').find((part) => part.startsWith(`${name}=`))
  return match ? decodeURIComponent(match.slice(name.length + 1)) : undefined
}

async function send(path: string, options: RequestOptions): Promise<Response> {
  const method = options.method ?? 'GET'
  const headers: Record<string, string> = { Accept: 'application/json' }
  let body: BodyInit | undefined
  if (method !== 'GET') {
    // Spring Security's SPA CSRF setup: echo the XSRF-TOKEN cookie back as a header.
    const token = readCookie('XSRF-TOKEN')
    if (token) headers['X-XSRF-TOKEN'] = token
  }
  if (options.json !== undefined) {
    headers['Content-Type'] = 'application/json'
    body = JSON.stringify(options.json)
  } else if (options.form) {
    body = new URLSearchParams(options.form)
  }
  return fetch(`/api${path}`, {
    method,
    headers,
    body,
    credentials: 'same-origin',
    signal: options.signal,
  })
}

async function toError(response: Response): Promise<ApiError> {
  const data = await response.json().catch(() => null)
  return new ApiError(
    response.status,
    data?.code ?? `HTTP_${response.status}`,
    data?.detail ?? response.statusText,
    data?.errors,
  )
}

/** Calls `/api{path}` and returns the parsed JSON (or `undefined` for 204). */
export async function api<T>(path: string, options: RequestOptions = {}): Promise<T> {
  let response: Response
  try {
    response = await send(path, options)
    if (response.status === 403 && options.method && options.method !== 'GET') {
      const error = await toError(response.clone())
      if (error.code === 'CSRF_INVALID') {
        // The token cookie was missing or stale: any GET issues a fresh one, then retry once.
        await fetch('/api/auth/me', { credentials: 'same-origin' })
        response = await send(path, options)
      }
    }
  } catch (cause) {
    if (cause instanceof DOMException && cause.name === 'AbortError') throw cause
    throw new ApiError(0, 'NETWORK_ERROR', 'Cannot reach the server.')
  }
  if (!response.ok) throw await toError(response)
  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}
