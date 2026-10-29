import {
  Alert,
  Button,
  Center,
  Group,
  Paper,
  PasswordInput,
  Stack,
  Text,
  Title,
} from '@mantine/core'
import { zodResolver } from '@hookform/resolvers/zod'
import { notifications } from '@mantine/notifications'
import { IconInfoCircle } from '@tabler/icons-react'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { z } from 'zod'
import { useChangePassword, useLogout } from '../api/auth'
import { AppLogo } from '../components/AppLogo'
import { LanguageSwitch } from '../components/HeaderControls'
import { PageHeader } from '../components/PageHeader'
import { errorMessage } from '../i18n/errors'

// Mirrors the server rules so most mistakes are caught before submitting.
const schema = z
  .object({
    currentPassword: z.string().min(1, 'validation.required'),
    newPassword: z
      .string()
      .min(8, 'validation.passwordLength')
      .max(72, 'validation.tooLong')
      .regex(/^(?=.*\p{L})(?=.*\d).+$/u, 'validation.passwordMix'),
    confirmPassword: z.string(),
  })
  .refine((values) => values.newPassword === values.confirmPassword, {
    path: ['confirmPassword'],
    message: 'validation.passwordMismatch',
  })

function ChangePasswordForm({ onDone }: { onDone: () => void }) {
  const { t } = useTranslation()
  const changePassword = useChangePassword()
  const form = useForm({
    resolver: zodResolver(schema),
    defaultValues: { currentPassword: '', newPassword: '', confirmPassword: '' },
  })
  const { errors } = form.formState

  return (
    <form
      noValidate
      onSubmit={form.handleSubmit(({ currentPassword, newPassword }) =>
        changePassword.mutate(
          { currentPassword, newPassword },
          {
            onSuccess: () => {
              notifications.show({ color: 'teal', message: t('password.success') })
              onDone()
            },
          },
        ),
      )}
    >
      <Stack>
        {changePassword.isError && (
          <Alert color="red" role="alert">
            {errorMessage(t, changePassword.error, 'password')}
          </Alert>
        )}
        <PasswordInput
          label={t('password.current')}
          autoComplete="current-password"
          error={errors.currentPassword?.message && t(errors.currentPassword.message)}
          {...form.register('currentPassword')}
        />
        <PasswordInput
          label={t('password.new')}
          description={t('password.rules')}
          autoComplete="new-password"
          error={errors.newPassword?.message && t(errors.newPassword.message)}
          {...form.register('newPassword')}
        />
        <PasswordInput
          label={t('password.confirm')}
          autoComplete="new-password"
          error={errors.confirmPassword?.message && t(errors.confirmPassword.message)}
          {...form.register('confirmPassword')}
        />
        <Group justify="flex-end">
          <Button type="submit" loading={changePassword.isPending}>
            {t('password.submit')}
          </Button>
        </Group>
      </Stack>
    </form>
  )
}

/** Shown on its own after signing in with a temporary password; nothing else is reachable. */
export function ForcedChangePasswordPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const logout = useLogout()
  return (
    <Center mih="100vh" p="md">
      <Stack w="100%" maw={440} gap="lg">
        <Group justify="space-between">
          <AppLogo />
          <LanguageSwitch />
        </Group>
        <Paper withBorder shadow="sm" p="xl" radius="lg">
          <Stack>
            <Title order={3}>{t('password.forcedTitle')}</Title>
            <Alert icon={<IconInfoCircle size={18} />}>{t('password.forcedInfo')}</Alert>
            <ChangePasswordForm onDone={() => navigate('/', { replace: true })} />
          </Stack>
        </Paper>
        <Button variant="subtle" color="gray" onClick={() => logout.mutate()}>
          {t('nav.signOut')}
        </Button>
      </Stack>
    </Center>
  )
}

export function ChangePasswordPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  return (
    <Stack maw={480}>
      <PageHeader title={t('password.title')} />
      <Paper withBorder p="lg" radius="md">
        <ChangePasswordForm onDone={() => navigate('/')} />
      </Paper>
      <Text size="xs" c="dimmed">
        {t('password.rules')}
      </Text>
    </Stack>
  )
}
