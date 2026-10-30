import {
  Alert,
  Box,
  Button,
  Code,
  Group,
  Paper,
  PasswordInput,
  SegmentedControl,
  Skeleton,
  Stack,
  Text,
  TextInput,
  Title,
} from '@mantine/core'
import { modals } from '@mantine/modals'
import { notifications } from '@mantine/notifications'
import { IconWifi } from '@tabler/icons-react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { Controller, useForm, useWatch } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { z } from 'zod'
import { ApiError } from '../api/client'
import { useClearWifi, useSaveWifi, useSettings, wifiQrUrl } from '../api/settings'
import type { Settings, WifiNetwork } from '../api/types'
import { PageHeader } from '../components/PageHeader'
import { LoadError } from '../components/PageStates'
import { errorMessage } from '../i18n/errors'
import { isLocalOnlyLink } from './camps/links'

const schema = z
  .object({
    ssid: z
      .string()
      .trim()
      .min(1, 'validation.required')
      // Wi-Fi names are limited to 32 bytes; Hindi letters take 3 bytes each.
      .refine((value) => new TextEncoder().encode(value).length <= 32, 'settings.wifi.nameTooLong'),
    security: z.enum(['WPA', 'NONE']),
    password: z.string(),
  })
  .superRefine((values, ctx) => {
    if (values.security === 'WPA' && !/^[\x20-\x7E]{8,63}$/.test(values.password))
      ctx.addIssue({ code: 'custom', path: ['password'], message: 'settings.wifi.passwordRule' })
  })

type FormValues = z.input<typeof schema>

export function SettingsPage() {
  const { t } = useTranslation()
  const settings = useSettings()

  return (
    <Stack maw={860}>
      <PageHeader title={t('settings.title')} subtitle={t('settings.subtitle')} />
      {settings.isPending ? (
        <Skeleton height={320} />
      ) : settings.isError ? (
        <LoadError
          message={errorMessage(t, settings.error)}
          onRetry={() => void settings.refetch()}
        />
      ) : (
        <>
          <LinksSection settings={settings.data} />
          <WifiSection key={JSON.stringify(settings.data.wifi)} wifi={settings.data.wifi} />
        </>
      )}
    </Stack>
  )
}

function LinksSection({ settings }: { settings: Settings }) {
  const { t } = useTranslation()
  const localOnly = isLocalOnlyLink(settings.publicBaseUrl)
  return (
    <Paper withBorder radius="md" p="lg">
      <Stack gap="sm">
        <Title order={4}>{t('settings.links.title')}</Title>
        <Text size="sm">{t('settings.links.body')}</Text>
        <Code fz="md" px="sm" py={6}>
          {settings.publicBaseUrl}/f/…
        </Code>
        {localOnly ? (
          <Alert color="orange" title={t('camps.qr.localTitle')}>
            <Stack gap={6}>
              <Text size="sm">{t('camps.qr.localBody')}</Text>
              <Code block>APP_PUBLIC_BASE_URL=http://192.168.137.1:8080</Code>
            </Stack>
          </Alert>
        ) : (
          <Text size="sm" c="dimmed">
            {t('settings.links.howToChange')}
          </Text>
        )}
      </Stack>
    </Paper>
  )
}

function WifiSection({ wifi }: { wifi: WifiNetwork | null }) {
  const { t } = useTranslation()
  const saveWifi = useSaveWifi()
  const clearWifi = useClearWifi()
  // Makes the preview image reload after each save.
  const [previewVersion, setPreviewVersion] = useState(() => Date.now())

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      ssid: wifi?.ssid ?? '',
      security: wifi?.security ?? 'WPA',
      password: wifi?.password ?? '',
    },
  })
  const { errors } = form.formState
  const security = useWatch({ control: form.control, name: 'security' })
  const fieldError = (message?: string) => (message ? t(message) : undefined)

  const submit = form.handleSubmit((values) => {
    const parsed = schema.parse(values)
    saveWifi.mutate(
      { ...parsed, password: parsed.security === 'WPA' ? parsed.password : '' },
      {
        onSuccess: () => {
          setPreviewVersion(Date.now())
          notifications.show({ color: 'teal', message: t('settings.wifi.saved') })
        },
        onError: (error) => {
          if (error instanceof ApiError && error.code === 'WIFI_PASSWORD_INVALID')
            form.setError('password', { message: 'settings.wifi.passwordRule' })
          else if (error instanceof ApiError && error.code === 'WIFI_NAME_TOO_LONG')
            form.setError('ssid', { message: 'settings.wifi.nameTooLong' })
        },
      },
    )
  })

  const confirmClear = () =>
    modals.openConfirmModal({
      title: t('settings.wifi.clearConfirm.title'),
      children: <Text size="sm">{t('settings.wifi.clearConfirm.body')}</Text>,
      labels: { confirm: t('settings.wifi.clear'), cancel: t('common.cancel') },
      confirmProps: { color: 'red' },
      onConfirm: () =>
        clearWifi.mutate(undefined, {
          onSuccess: () =>
            notifications.show({ color: 'teal', message: t('settings.wifi.cleared') }),
          onError: (error) =>
            notifications.show({ color: 'red', message: errorMessage(t, error, 'settings') }),
        }),
    })

  const showAlert =
    saveWifi.isError &&
    !(
      saveWifi.error instanceof ApiError &&
      ['VALIDATION_FAILED', 'WIFI_PASSWORD_INVALID', 'WIFI_NAME_TOO_LONG'].includes(
        saveWifi.error.code,
      )
    )

  return (
    <Paper withBorder radius="md" p="lg">
      <Group align="flex-start" gap="xl">
        <form noValidate onSubmit={submit} style={{ flex: '1 1 320px' }}>
          <Stack>
            <Stack gap={4}>
              <Title order={4}>{t('settings.wifi.title')}</Title>
              <Text size="sm" c="dimmed">
                {t('settings.wifi.body')}
              </Text>
            </Stack>
            {showAlert && (
              <Alert color="red" role="alert">
                {errorMessage(t, saveWifi.error, 'settings')}
              </Alert>
            )}
            <TextInput
              label={t('settings.wifi.ssid')}
              description={t('settings.wifi.ssidHint')}
              withAsterisk
              error={fieldError(errors.ssid?.message)}
              {...form.register('ssid')}
            />
            <Controller
              control={form.control}
              name="security"
              render={({ field }) => (
                <Stack gap={4}>
                  <Text size="sm" fw={500}>
                    {t('settings.wifi.security')}
                  </Text>
                  <SegmentedControl
                    aria-label={t('settings.wifi.security')}
                    value={field.value}
                    onChange={field.onChange}
                    data={[
                      { value: 'WPA', label: t('settings.wifi.withPassword') },
                      { value: 'NONE', label: t('settings.wifi.open') },
                    ]}
                  />
                </Stack>
              )}
            />
            {security === 'WPA' && (
              <PasswordInput
                label={t('settings.wifi.password')}
                description={t('settings.wifi.passwordHint')}
                withAsterisk
                error={fieldError(errors.password?.message)}
                {...form.register('password')}
              />
            )}
            <Group>
              <Button type="submit" loading={saveWifi.isPending}>
                {t('common.save')}
              </Button>
              {wifi && (
                <Button
                  variant="subtle"
                  color="red"
                  loading={clearWifi.isPending}
                  onClick={confirmClear}
                >
                  {t('settings.wifi.clear')}
                </Button>
              )}
            </Group>
          </Stack>
        </form>
        <Stack align="center" gap="xs" w={200}>
          {wifi ? (
            <>
              <Box bg="white" p={4} style={{ borderRadius: 8, lineHeight: 0 }}>
                <img
                  src={wifiQrUrl(400, previewVersion)}
                  width={180}
                  height={180}
                  alt={t('settings.wifi.previewAlt', { ssid: wifi.ssid })}
                />
              </Box>
              <Text size="xs" c="dimmed" ta="center">
                {t('settings.wifi.previewHint')}
              </Text>
            </>
          ) : (
            <Stack align="center" gap="xs" py="lg">
              <IconWifi size={48} stroke={1.3} color="var(--mantine-color-dimmed)" />
              <Text size="sm" c="dimmed" ta="center">
                {t('settings.wifi.notSet')}
              </Text>
            </Stack>
          )}
        </Stack>
      </Group>
    </Paper>
  )
}
