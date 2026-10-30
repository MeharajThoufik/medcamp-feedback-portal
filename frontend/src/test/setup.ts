import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach, vi } from 'vitest'
import '../i18n'

// jsdom lacks these browser APIs, which Mantine uses.
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation((query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })),
})

class ResizeObserverStub {
  observe() {}
  unobserve() {}
  disconnect() {}
}
window.ResizeObserver = ResizeObserverStub as unknown as typeof ResizeObserver

window.HTMLElement.prototype.scrollIntoView = () => {}
window.scrollTo = () => {}

// The auto-growing textarea listens for web fonts loading.
Object.defineProperty(document, 'fonts', {
  configurable: true,
  value: { addEventListener: vi.fn(), removeEventListener: vi.fn(), ready: Promise.resolve() },
})

afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})
