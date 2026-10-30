import {
  ActionIcon,
  Alert,
  Anchor,
  Button,
  Grid,
  Group,
  Paper,
  Select,
  Stack,
  Text,
  TextInput,
  Title,
} from '@mantine/core'
import { DatePickerInput } from '@mantine/dates'
import { notifications } from '@mantine/notifications'
import {
  IconArrowDown,
  IconArrowLeft,
  IconArrowUp,
  IconCalendar,
  IconPlus,
  IconTrash,
} from '@tabler/icons-react'
import { zodResolver } from '@hookform/resolvers/zod'
import { Controller, useFieldArray, useForm, type FieldPath } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate, useParams } from 'react-router'
import { z } from 'zod'
import {
  useCamp,
  useCreateCamp,
  useOrganizers,
  useUpdateCamp,
  type CampInput,
} from '../../api/camps'
import { ApiError } from '../../api/client'
import { useDepartments } from '../../api/departments'
import type { Camp } from '../../api/types'
import { useCurrentUser } from '../../auth/useCurrentUser'
import { errorMessage } from '../../i18n/errors'
import { ForbiddenPage } from '../ErrorPages'
import { CampLoadError, CampPageSkeleton } from './CampLoadState'
import { defaultFeedbackWindow } from './campDates'

const MAX_SERVICES = 50

const dateRange = (message: string) =>
  z
    .tuple([z.string().nullable(), z.string().nullable()])
    .refine(([from, to]) => Boolean(from && to), message)

function makeSchema(needsOrganizer: boolean) {
  return z
    .object({
      name: z.string().trim().min(1, 'validation.required').max(150, 'validation.tooLong'),
      location: z.string().trim().min(1, 'validation.required').max(200, 'validation.tooLong'),
      dates: dateRange('camps.validation.datesRequired'),
      feedbackWindow: dateRange('camps.validation.windowRequired'),
      organizerId: z
        .string()
        .nullable()
        .refine((value) => !needsOrganizer || Boolean(value), 'validation.required'),
      services: z
        .array(
          z.object({
            serviceId: z.number().optional(),
            departmentId: z
              .string()
              .nullable()
              .refine((value) => value !== null, 'validation.required'),
            name: z.string().trim().min(1, 'validation.required').max(100, 'validation.tooLong'),
          }),
        )
        .max(MAX_SERVICES, 'camps.validation.tooManyServices')
        .superRefine((services, ctx) => {
          const seen = new Set<string>()
          services.forEach((service, index) => {
            const key = service.name.toLowerCase()
            if (seen.has(key))
              ctx.addIssue({
                code: 'custom',
                path: [index, 'name'],
                message: 'camps.validation.duplicateService',
              })
            seen.add(key)
          })
        }),
    })
    .superRefine((values, ctx) => {
      const [start] = values.dates
      const [, closes] = values.feedbackWindow
      if (start && closes && closes < start)
        ctx.addIssue({
          code: 'custom',
          path: ['feedbackWindow'],
          message: 'camps.validation.windowBeforeCamp',
        })
    })
}

type FormValues = z.input<ReturnType<typeof makeSchema>>

/** Server field names and error codes, mapped to the form field they belong to. */
const SERVER_FIELDS: Record<string, FieldPath<FormValues>> = {
  name: 'name',
  location: 'location',
  startDate: 'dates',
  endDate: 'dates',
  feedbackOpensOn: 'feedbackWindow',
  feedbackClosesOn: 'feedbackWindow',
  organizerId: 'organizerId',
}
const CODE_FIELDS: Record<string, FieldPath<FormValues>> = {
  INVALID_DATES: 'dates',
  INVALID_FEEDBACK_WINDOW: 'feedbackWindow',
  ORGANIZER_REQUIRED: 'organizerId',
  ORGANIZER_INVALID: 'organizerId',
}

function toFormValues(camp: Camp | null): FormValues {
  return {
    name: camp?.name ?? '',
    location: camp?.location ?? '',
    dates: [camp?.startDate ?? null, camp?.endDate ?? null],
    feedbackWindow: [camp?.feedbackOpensOn ?? null, camp?.feedbackClosesOn ?? null],
    organizerId: camp ? String(camp.organizerId) : null,
    services: (camp?.services ?? []).map((s) => ({
      serviceId: s.id,
      departmentId: String(s.departmentId),
      name: s.name,
    })),
  }
}

export function NewCampPage() {
  return <CampForm camp={null} />
}

export function EditCampPage() {
  const { t } = useTranslation()
  const id = Number(useParams().id)
  const camp = useCamp(id)

  if (camp.isPending) return <CampPageSkeleton />
  if (camp.isError) return <CampLoadError error={camp.error} onRetry={() => void camp.refetch()} />
  if (!camp.data.canEdit) return <ForbiddenPage />
  if (camp.data.status === 'ARCHIVED')
    return (
      <Alert color="gray" title={t('camps.form.archivedTitle')} maw={640}>
        <Stack gap="sm" align="flex-start">
          <Text size="sm">{t('camps.form.archivedBody')}</Text>
          <Button component={Link} to={`/camps/${id}`} size="xs" variant="light">
            {t('camps.backToCamp')}
          </Button>
        </Stack>
      </Alert>
    )
  return <CampForm key={camp.data.id} camp={camp.data} />
}

function CampForm({ camp }: { camp: Camp | null }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const me = useCurrentUser()
  const isAdmin = me.role === 'SUPER_ADMIN'
  const isNew = camp === null

  const departments = useDepartments()
  const organizers = useOrganizers(isAdmin)
  const createCamp = useCreateCamp()
  const updateCamp = useUpdateCamp()
  const mutation = isNew ? createCamp : updateCamp

  const schema = makeSchema(isAdmin)
  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: toFormValues(camp),
  })
  const { errors } = form.formState
  const services = useFieldArray({ control: form.control, name: 'services' })
  const fieldError = (message?: string) => (message ? t(message) : undefined)

  // Keep departments and organizers that were deactivated later selectable for this camp.
  const departmentOptions = (departments.data ?? []).map((d) => ({
    value: String(d.id),
    label: d.name,
  }))
  for (const service of camp?.services ?? []) {
    if (!departmentOptions.some((o) => o.value === String(service.departmentId)))
      departmentOptions.push({ value: String(service.departmentId), label: service.departmentName })
  }
  const organizerOptions = (organizers.data ?? []).map((o) => ({
    value: String(o.id),
    label: `${o.fullName} (${o.email})`,
  }))
  if (camp && !organizerOptions.some((o) => o.value === String(camp.organizerId)))
    organizerOptions.push({ value: String(camp.organizerId), label: camp.organizerName })

  const addService = () => {
    const rows = form.getValues('services')
    // Most camps list several services per department, so start with the previous row's.
    services.append(
      { departmentId: rows.at(-1)?.departmentId ?? null, name: '' },
      { focusName: `services.${rows.length}.name` },
    )
  }

  const submit = form.handleSubmit((values) => {
    const parsed = schema.parse(values)
    const input: CampInput = {
      name: parsed.name,
      location: parsed.location,
      startDate: parsed.dates[0]!,
      endDate: parsed.dates[1]!,
      feedbackOpensOn: parsed.feedbackWindow[0]!,
      feedbackClosesOn: parsed.feedbackWindow[1]!,
      organizerId: isAdmin && parsed.organizerId ? Number(parsed.organizerId) : null,
      services: parsed.services.map((s) => ({
        id: s.serviceId,
        departmentId: Number(s.departmentId),
        name: s.name,
      })),
    }
    const onSuccess = (saved: Camp) => {
      notifications.show({
        color: 'teal',
        message: isNew ? t('camps.form.created') : t('camps.form.updated'),
      })
      navigate(`/camps/${saved.id}`)
    }
    const onError = (error: Error) => {
      if (!(error instanceof ApiError)) return
      for (const [field, message] of Object.entries(error.fieldErrors)) {
        const serviceField = /^services\[(\d+)\]\.(name|departmentId)$/.exec(field)
        const path = serviceField
          ? (`services.${Number(serviceField[1])}.${serviceField[2]}` as FieldPath<FormValues>)
          : SERVER_FIELDS[field]
        if (path) form.setError(path, { message })
      }
      const codeField = CODE_FIELDS[error.code]
      if (codeField) form.setError(codeField, { message: `camps.errors.${error.code}` })
    }
    if (camp) updateCamp.mutate({ id: camp.id, input }, { onSuccess, onError })
    else createCamp.mutate(input, { onSuccess, onError })
  })

  const error = mutation.error
  const showErrorAlert =
    mutation.isError &&
    !(error instanceof ApiError && (error.code === 'VALIDATION_FAILED' || CODE_FIELDS[error.code]))
  const backTo = camp ? `/camps/${camp.id}` : '/camps'

  return (
    <form noValidate onSubmit={submit}>
      <Stack maw={860} gap="lg">
        <Stack gap={4}>
          <Anchor component={Link} to={backTo} size="sm" c="dimmed">
            <Group gap={4}>
              <IconArrowLeft size={14} />
              {camp ? t('camps.backToCamp') : t('camps.backToList')}
            </Group>
          </Anchor>
          <Title order={2}>{isNew ? t('camps.form.createTitle') : t('camps.form.editTitle')}</Title>
        </Stack>

        {showErrorAlert && (
          <Alert color="red" role="alert">
            {errorMessage(t, error, 'camps')}
          </Alert>
        )}

        <Paper withBorder radius="md" p="lg">
          <Stack>
            <Title order={4}>{t('camps.form.detailsSection')}</Title>
            <TextInput
              label={t('camps.form.name')}
              placeholder={t('camps.form.namePlaceholder')}
              withAsterisk
              data-autofocus
              error={fieldError(errors.name?.message)}
              {...form.register('name')}
            />
            <TextInput
              label={t('camps.form.location')}
              placeholder={t('camps.form.locationPlaceholder')}
              withAsterisk
              error={fieldError(errors.location?.message)}
              {...form.register('location')}
            />
            {isAdmin && (
              <Controller
                control={form.control}
                name="organizerId"
                render={({ field }) => (
                  <Select
                    label={t('camps.form.organizer')}
                    description={t('camps.form.organizerHint')}
                    placeholder={t('camps.form.organizerPlaceholder')}
                    withAsterisk
                    searchable
                    data={organizerOptions}
                    value={field.value}
                    onChange={field.onChange}
                    onBlur={field.onBlur}
                    nothingFoundMessage={t('camps.form.noOrganizers')}
                    error={fieldError(errors.organizerId?.message)}
                  />
                )}
              />
            )}
          </Stack>
        </Paper>

        <Paper withBorder radius="md" p="lg">
          <Stack>
            <Title order={4}>{t('camps.form.datesSection')}</Title>
            <Grid>
              <Grid.Col span={{ base: 12, sm: 6 }}>
                <Controller
                  control={form.control}
                  name="dates"
                  render={({ field }) => (
                    <DatePickerInput
                      type="range"
                      allowSingleDateInRange
                      label={t('camps.form.campDates')}
                      description={t('camps.form.campDatesHint')}
                      placeholder={t('camps.form.pickDates')}
                      leftSection={<IconCalendar size={16} />}
                      valueFormat="D MMM YYYY"
                      withAsterisk
                      value={field.value}
                      onChange={(value) => {
                        const range = value as [string | null, string | null]
                        field.onChange(range)
                        // Suggest a window for a new camp until the organizer picks their own.
                        if (
                          isNew &&
                          range[0] &&
                          range[1] &&
                          !form.getFieldState('feedbackWindow').isDirty
                        )
                          form.setValue('feedbackWindow', defaultFeedbackWindow(range[0], range[1]))
                      }}
                      onBlur={field.onBlur}
                      error={fieldError(errors.dates?.message)}
                    />
                  )}
                />
              </Grid.Col>
              <Grid.Col span={{ base: 12, sm: 6 }}>
                <Controller
                  control={form.control}
                  name="feedbackWindow"
                  render={({ field }) => (
                    <DatePickerInput
                      type="range"
                      allowSingleDateInRange
                      label={t('camps.form.feedbackWindow')}
                      description={t('camps.form.feedbackWindowHint')}
                      placeholder={t('camps.form.pickDates')}
                      leftSection={<IconCalendar size={16} />}
                      valueFormat="D MMM YYYY"
                      withAsterisk
                      value={field.value}
                      onChange={(value) => field.onChange(value)}
                      onBlur={field.onBlur}
                      error={fieldError(errors.feedbackWindow?.message)}
                    />
                  )}
                />
              </Grid.Col>
            </Grid>
          </Stack>
        </Paper>

        <Paper withBorder radius="md" p="lg">
          <Stack>
            <Stack gap={4}>
              <Title order={4}>{t('camps.form.servicesSection')}</Title>
              <Text size="sm" c="dimmed">
                {t('camps.form.servicesHint')}
              </Text>
            </Stack>

            {services.fields.length === 0 && (
              <Text size="sm" c="orange">
                {t('camps.form.noServicesYet')}
              </Text>
            )}
            {departments.isSuccess && departments.data.length === 0 && (
              <Alert color="orange">
                {isAdmin ? (
                  <>
                    {t('camps.form.noDepartmentsAdmin')}{' '}
                    <Anchor component={Link} to="/departments">
                      {t('nav.departments')}
                    </Anchor>
                  </>
                ) : (
                  t('camps.form.noDepartments')
                )}
              </Alert>
            )}

            {services.fields.map((row, index) => {
              const rowErrors = errors.services?.[index]
              return (
                <Grid key={row.id} align="flex-start" gap="xs">
                  <Grid.Col span={{ base: 12, sm: 4 }}>
                    <Controller
                      control={form.control}
                      name={`services.${index}.departmentId`}
                      render={({ field }) => (
                        <Select
                          aria-label={t('camps.form.serviceDepartment')}
                          placeholder={t('camps.form.serviceDepartment')}
                          searchable
                          allowDeselect={false}
                          data={departmentOptions}
                          value={field.value}
                          onChange={field.onChange}
                          onBlur={field.onBlur}
                          error={fieldError(rowErrors?.departmentId?.message)}
                        />
                      )}
                    />
                  </Grid.Col>
                  <Grid.Col span={{ base: 12, sm: 'auto' }}>
                    <TextInput
                      aria-label={t('camps.form.serviceName')}
                      placeholder={t('camps.form.serviceNamePlaceholder')}
                      error={fieldError(rowErrors?.name?.message)}
                      {...form.register(`services.${index}.name`)}
                    />
                  </Grid.Col>
                  <Grid.Col span={{ base: 12, sm: 'content' }}>
                    <Group gap={4} justify="flex-end" wrap="nowrap" mt={4}>
                      <ActionIcon
                        variant="subtle"
                        color="gray"
                        aria-label={t('camps.form.moveUp')}
                        disabled={index === 0}
                        onClick={() => services.move(index, index - 1)}
                      >
                        <IconArrowUp size={16} />
                      </ActionIcon>
                      <ActionIcon
                        variant="subtle"
                        color="gray"
                        aria-label={t('camps.form.moveDown')}
                        disabled={index === services.fields.length - 1}
                        onClick={() => services.move(index, index + 1)}
                      >
                        <IconArrowDown size={16} />
                      </ActionIcon>
                      <ActionIcon
                        variant="subtle"
                        color="red"
                        aria-label={t('camps.form.removeService')}
                        onClick={() => services.remove(index)}
                      >
                        <IconTrash size={16} />
                      </ActionIcon>
                    </Group>
                  </Grid.Col>
                </Grid>
              )
            })}

            {errors.services?.root?.message && (
              <Text size="sm" c="red">
                {t(errors.services.root.message)}
              </Text>
            )}
            {errors.services?.message && (
              <Text size="sm" c="red">
                {t(errors.services.message)}
              </Text>
            )}

            <Group>
              <Button
                variant="light"
                leftSection={<IconPlus size={16} />}
                onClick={addService}
                disabled={services.fields.length >= MAX_SERVICES}
              >
                {t('camps.form.addService')}
              </Button>
            </Group>
          </Stack>
        </Paper>

        <Group justify="flex-end">
          <Button variant="default" component={Link} to={backTo}>
            {t('common.cancel')}
          </Button>
          <Button type="submit" loading={mutation.isPending}>
            {isNew ? t('camps.form.createSubmit') : t('common.save')}
          </Button>
        </Group>
      </Stack>
    </form>
  )
}
