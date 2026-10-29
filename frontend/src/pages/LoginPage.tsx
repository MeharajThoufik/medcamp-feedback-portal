import {
  Alert,
  Box,
  Button,
  Center,
  Group,
  Paper,
  PasswordInput,
  Stack,
  Text,
  TextInput,
  Title,
} from '@mantine/core'
import { zodResolver } from '@hookform/resolvers/zod'
import { IconAlertCircle } from '@tabler/icons-react'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { Navigate, useLocation } from 'react-router'
import { z } from 'zod'
import { useLogin, useMe } from '../api/auth'
import { AppLogo } from '../components/AppLogo'
import { ColorSchemeToggle, LanguageSwitch } from '../components/HeaderControls'
import { FullPageLoader } from '../components/PageStates'
import { errorMessage } from '../i18n/errors'

const schema = z.object({
  email: z.string().trim().min(1, 'validation.required'),
  password: z.string().min(1, 'validation.required'),
})

export function LoginPage() {
  const { t } = useTranslation()
  const { data: me, isPending } = useMe()
  const location = useLocation()
  const login = useLogin()
  const form = useForm({
    resolver: zodResolver(schema),
    defaultValues: { email: '', password: '' },
  })
  const { errors } = form.formState

  if (isPending) return <FullPageLoader />
  if (me) {
    const from = (location.state as { from?: string } | null)?.from
    return <Navigate to={from && from !== '/login' ? from : '/'} replace />
  }

  return (
    <Box mih="100vh" bg="var(--mantine-color-body)">
      <Group justify="flex-end" p="md" gap="xs">
        <LanguageSwitch />
        <ColorSchemeToggle />
      </Group>
      <Center px="md" pb="xl">
        <Stack w="100%" maw={400} gap="lg">
          <Center>
            <AppLogo size="lg" />
          </Center>
          <Paper withBorder shadow="sm" p="xl" radius="lg">
            <form noValidate onSubmit={form.handleSubmit((values) => login.mutate(values))}>
              <Stack>
                <div>
                  <Title order={3}>{t('login.title')}</Title>
                  <Text size="sm" c="dimmed" mt={4}>
                    {t('login.subtitle')}
                  </Text>
                </div>
                {login.isError && (
                  <Alert color="red" icon={<IconAlertCircle size={18} />} role="alert">
                    {errorMessage(t, login.error, 'login')}
                  </Alert>
                )}
                <TextInput
                  label={t('login.email')}
                  type="email"
                  autoComplete="username"
                  autoFocus
                  size="md"
                  error={errors.email?.message && t(errors.email.message)}
                  {...form.register('email')}
                />
                <PasswordInput
                  label={t('login.password')}
                  autoComplete="current-password"
                  size="md"
                  error={errors.password?.message && t(errors.password.message)}
                  {...form.register('password')}
                />
                <Button type="submit" size="md" loading={login.isPending} fullWidth mt="xs">
                  {t('login.submit')}
                </Button>
              </Stack>
            </form>
          </Paper>
          <Text size="xs" c="dimmed" ta="center">
            {t('app.name')}
          </Text>
        </Stack>
      </Center>
    </Box>
  )
}
