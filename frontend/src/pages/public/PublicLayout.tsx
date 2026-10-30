import { Container, Group, Stack } from '@mantine/core'
import type { ReactNode } from 'react'
import { AppLogo } from '../../components/AppLogo'
import { LanguageSwitch } from '../../components/HeaderControls'

/** The attendee pages: logo and language switch only, no staff menu. Built for phones. */
export function PublicLayout({ children }: { children: ReactNode }) {
  return (
    <Stack mih="100vh" gap={0} bg="var(--mantine-color-body)">
      <Group
        justify="space-between"
        px="md"
        py="sm"
        style={{ borderBottom: '1px solid var(--mantine-color-default-border)' }}
      >
        <AppLogo />
        <LanguageSwitch />
      </Group>
      <Container size={560} w="100%" px="md" py="lg">
        {children}
      </Container>
    </Stack>
  )
}
