<template>
  <!--
    The rows of a sheet as the file holds them, and above each column what it is taken to be. The
    columns the user did not touch say so, so that a default is never mistaken for a choice.
  -->
  <n-data-table
    size="small"
    :columns="columns"
    :data="rows"
    :scroll-x="scrollWidth"
    :max-height="360"
    :bordered="true"
  />
</template>

<script setup lang="ts">
import { computed, h, inject } from 'vue'
import type { ChoiceDTO, SheetDescriptionDTO, TypePropertyDTO } from '../lib'
import type { OpenSilexPlugin } from '../opensilexPlugin'

const ROLE_OBJECT_PROPERTY = 'OBJECT_PROPERTY'
const COLUMN_WIDTH = 190

//#region Public
const emit = defineEmits<{
  role: [header: string, role: string]
  property: [header: string, property: string]
}>()

const props = defineProps<{
  sheet: SheetDescriptionDTO
  /** The roles the kind of this sheet accepts, in the order they are offered. */
  roles: ChoiceDTO[]
  /** The properties of the sheet's type, for the columns described as properties. */
  properties: TypePropertyDTO[]
}>()
//#endregion

//#region Private

//#region Plugins & services
const $opensilex = inject<OpenSilexPlugin>('$opensilex')!
//#endregion

//#region Data & Computed
const rows = computed(() => (props.sheet.rows ?? []).map((row) => ({ ...row.values, __row: row.row })))

const scrollWidth = computed(() => ((props.sheet.headers?.length ?? 0) + 1) * COLUMN_WIDTH)

const columns = computed(() => [
  { title: t('AiImport.rows.row'), key: '__row', width: 70, fixed: 'left' as const },
  ...(props.sheet.headers ?? []).map((header) => ({
    key: header,
    width: COLUMN_WIDTH,
    ellipsis: { tooltip: true },
    title: () => headerCell(header)
  }))
])
//#endregion

//#region Methods
function t(key: string): string {
  return String($opensilex.$i18n.t(key))
}

function isChosen(header: string): boolean {
  return (props.sheet.roles_chosen ?? []).includes(header)
}

/**
 * The header of a column, the choice of its role under it, and under that the property it is
 * written to when it is described as one.
 */
function headerCell(header: string) {
  const role = props.sheet.roles?.[header] ?? ''
  const cell = [
    h('div', { class: 'ai-import-preview__column' }, header),
    h(
      'select',
      {
        class: ['ai-import-select', { 'ai-import-select--chosen': isChosen(header) }],
        title: isChosen(header) ? undefined : t('AiImport.advanced.defaultRole'),
        onChange: (event: Event) => emit('role', header, (event.target as HTMLSelectElement).value)
      },
      props.roles.map((choice) =>
        h('option', { value: choice.value, selected: choice.value === role }, t(choice.label_key ?? ''))
      )
    )
  ]
  if (role === ROLE_OBJECT_PROPERTY) {
    cell.push(propertySelect(header))
  }
  return h('div', { class: 'ai-import-preview__header' }, cell)
}

function propertySelect(header: string) {
  const chosen = props.sheet.properties?.[header] ?? ''
  return h(
    'select',
    {
      class: 'ai-import-select',
      disabled: !props.sheet.rdf_type,
      title: props.sheet.rdf_type ? undefined : t('AiImport.advanced.chooseTypeFirst'),
      onChange: (event: Event) => emit('property', header, (event.target as HTMLSelectElement).value)
    },
    [
      h('option', { value: '', selected: chosen === '' }, t('AiImport.advanced.noProperty')),
      ...props.properties.map((property) =>
        h('option', { value: property.uri, selected: property.uri === chosen }, property.name)
      )
    ]
  )
}
//#endregion

//#endregion
</script>

<style scoped lang="scss">
:deep(.ai-import-preview__header) {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

:deep(.ai-import-preview__column) {
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:deep(.ai-import-select) {
  width: 100%;
  padding: 0.15rem 0.3rem;
  font-size: 0.8rem;
  border: 1px solid var(--bs-border-color, #ced4da);
  border-radius: 0.25rem;
  background: var(--bs-body-bg, #fff);
  color: inherit;

}

:deep(.ai-import-select--chosen) {
  border-color: var(--bs-primary, #0d6efd);
}
</style>
