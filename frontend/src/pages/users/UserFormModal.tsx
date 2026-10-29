import { Alert, Button, Group, Modal, Select, Stack, Text, TextInput } from '@mantine/core'
import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { z } from 'zod'
import { ApiError } from '../../api/client'
import { useDepartments } from '../../api/departments'
import { ROLES, type User } from '../../api/types'
import { useCreateUser, useUpdateUser, type UserInput } from '../../api/users'
import { errorMessage } from '../../i18n/errors'

const schema = z
  .object({
    email: z
      .string()
      .trim()
      .min(1, 'validation.required')
      .max(255, 'validation.tooLong')
      .pipe(z.email('validation.email')),
    fullName: z.string().trim().min(1, 'validation.required').max(150, 'validation.tooLong'),
    role: z.enum(ROLES, { message: 'validation.required' }),
    departmentId: z.string().nullable(),
  })
  .refine((values) => values.role !== 'MEDICAL_STAFF' || values.departmentId, {
    path: ['departmentId'],
    message: 'validation.departmentRequired',
  })

type FormValues = z.input<typeof schema>

const FIELDS = ['email', 'fullName', 'role', 'departmentId'] as const

interface Props {
  opened: boolean
  /** The user being edited, or null to create a new one. */
  user: User | null
  currentUserId: number
  onClose: () => void
  onCreated: (user: User, temporaryPassword: string) => void
  onUpdated: () => void
}

export function UserFormModal({
  opened,
  user,
  currentUserId,
  onClose,
  onCreated,
  onUpdated,
}: Props) {
  const { t } = useTranslation()
  const departments = useDepartments()
  const createUser = useCreateUser()
  const updateUser = useUpdateUser()
  const mutation = user ? updateUser : createUser

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: '', fullName: '', role: undefined, departmentId: null },
  })
  const { errors } = form.formState
  const role = form.watch('role')

  useEffect(() => {
    if (!opened) return
    createUser.reset()
    updateUser.reset()
    form.reset({
      email: user?.email ?? '',
      fullName: user?.fullName ?? '',
      role: user?.role,
      departmentId: user?.departmentId != null ? String(user.departmentId) : null,
    })
    // Reset only when the dialog opens or switches user.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [opened, user])

  // Keep a department that was deactivated later selectable for the user who still has it.
  const departmentOptions = (departments.data ?? []).map((d) => ({
    value: String(d.id),
    label: d.name,
  }))
  if (
    user?.departmentId != null &&
    !departmentOptions.some((o) => o.value === String(user.departmentId))
  ) {
    departmentOptions.push({ value: String(user.departmentId), label: user.departmentName ?? '' })
  }

  const submit = form.handleSubmit((values) => {
    const parsed = schema.parse(values)
    const input: UserInput = {
      email: parsed.email,
      fullName: parsed.fullName,
      role: parsed.role,
      departmentId:
        parsed.role === 'MEDICAL_STAFF' && parsed.departmentId ? Number(parsed.departmentId) : null,
    }
    const onError = (error: Error) => {
      // Show server-side field errors next to the matching inputs.
      if (error instanceof ApiError) {
        for (const field of FIELDS) {
          const message = error.fieldErrors[field]
          if (message) form.setError(field, { message })
        }
        if (error.code === 'EMAIL_TAKEN')
          form.setError('email', { message: 'users.errors.EMAIL_TAKEN' })
      }
    }
    if (user) {
      updateUser.mutate({ id: user.id, input }, { onSuccess: onUpdated, onError })
    } else {
      createUser.mutate(input, {
        onSuccess: (result) => onCreated(result.user, result.temporaryPassword),
        onError,
      })
    }
  })

  const isSelf = user?.id === currentUserId
  const fieldError = (message?: string) => (message ? t(message) : undefined)

  return (
    <Modal
      opened={opened}
      onClose={onClose}
      title={user ? t('users.form.editTitle') : t('users.form.createTitle')}
      centered
    >
      <form noValidate onSubmit={submit}>
        <Stack>
          {mutation.isError &&
            !(
              mutation.error instanceof ApiError && mutation.error.code === 'VALIDATION_FAILED'
            ) && (
              <Alert color="red" role="alert">
                {errorMessage(t, mutation.error, 'users')}
              </Alert>
            )}
          <TextInput
            label={t('users.form.fullName')}
            withAsterisk
            data-autofocus
            error={fieldError(errors.fullName?.message)}
            {...form.register('fullName')}
          />
          <TextInput
            label={t('users.form.email')}
            type="email"
            withAsterisk
            error={fieldError(errors.email?.message)}
            {...form.register('email')}
          />
          <Controller
            control={form.control}
            name="role"
            render={({ field }) => (
              <Select
                label={t('users.form.role')}
                withAsterisk
                allowDeselect={false}
                disabled={isSelf}
                data={ROLES.map((r) => ({ value: r, label: t(`roles.${r}`) }))}
                value={field.value ?? null}
                onChange={(value) => field.onChange(value ?? undefined)}
                onBlur={field.onBlur}
                error={fieldError(errors.role?.message)}
              />
            )}
          />
          {role === 'MEDICAL_STAFF' && (
            <Controller
              control={form.control}
              name="departmentId"
              render={({ field }) => (
                <Select
                  label={t('users.form.department')}
                  description={t('users.form.departmentHint')}
                  placeholder={t('users.form.departmentPlaceholder')}
                  withAsterisk
                  searchable
                  data={departmentOptions}
                  value={field.value}
                  onChange={field.onChange}
                  onBlur={field.onBlur}
                  nothingFoundMessage={t('users.form.noDepartments')}
                  error={fieldError(errors.departmentId?.message)}
                />
              )}
            />
          )}
          {role === 'MEDICAL_STAFF' && departments.isSuccess && departments.data.length === 0 && (
            <Text size="sm" c="orange">
              {t('users.form.noDepartments')}
            </Text>
          )}
          {!user && (
            <Text size="sm" c="dimmed">
              {t('users.form.createHint')}
            </Text>
          )}
          <Group justify="flex-end" mt="sm">
            <Button variant="default" onClick={onClose}>
              {t('common.cancel')}
            </Button>
            <Button type="submit" loading={mutation.isPending}>
              {user ? t('common.save') : t('common.create')}
            </Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  )
}
