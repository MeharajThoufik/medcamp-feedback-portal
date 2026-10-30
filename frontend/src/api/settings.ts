import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import type { Settings, WifiNetwork, WifiSecurity } from './types'

const settingsKey = ['settings'] as const

/** Only Super Admins can load settings. */
export function useSettings() {
  return useQuery({ queryKey: settingsKey, queryFn: () => api<Settings>('/settings') })
}

export interface WifiInput {
  ssid: string
  password: string
  security: WifiSecurity
}

function useInvalidateSettings() {
  const queryClient = useQueryClient()
  return () => {
    void queryClient.invalidateQueries({ queryKey: settingsKey })
    // Posters show the Wi-Fi details.
    void queryClient.invalidateQueries({ queryKey: ['camps', 'poster'] })
  }
}

export function useSaveWifi() {
  const invalidate = useInvalidateSettings()
  return useMutation({
    mutationFn: (input: WifiInput) =>
      api<WifiNetwork>('/settings/wifi', { method: 'PUT', json: input }),
    onSuccess: invalidate,
  })
}

export function useClearWifi() {
  const invalidate = useInvalidateSettings()
  return useMutation({
    mutationFn: () => api<void>('/settings/wifi', { method: 'DELETE' }),
    onSuccess: invalidate,
  })
}

/**
 * The server never lets this image be cached; `version` makes an already shown image reload after
 * the Wi-Fi settings change.
 */
export function wifiQrUrl(size = 512, version = 0) {
  return `/api/settings/wifi/qr.png?${new URLSearchParams({ size: String(size), v: String(version) })}`
}
