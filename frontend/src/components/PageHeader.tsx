import { Group, Stack, Text, Title } from '@mantine/core'
import type { ReactNode } from 'react'

export function PageHeader({
  title,
  subtitle,
  action,
}: {
  title: string
  subtitle?: string
  action?: ReactNode
}) {
  return (
    <Group justify="space-between" align="flex-start" mb="lg" gap="sm">
      <Stack gap={4}>
        <Title order={2}>{title}</Title>
        {subtitle && <Text c="dimmed">{subtitle}</Text>}
      </Stack>
      {action}
    </Group>
  )
}
