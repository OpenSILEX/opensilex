<template>
  <n-data-table
    class="v-step-skos-relation-table"
    :columns="columns"
    :data="rows"
    :bordered="true"
    :single-line="false"
    size="small"
  />
</template>

<script setup lang="ts">
import { ref, watch, h } from 'vue'
import { useI18n } from 'vue-i18n'
import { NDataTable, NButton } from 'naive-ui'
import SkosSelector from './SkosSelector.vue'

/** Types */
type UriSkosRelation = {
  relationDtoKey: string
  uri: string
}

/** Props / Emits */
const props = defineProps<{
  /** v-model:uriRelations */
  uriRelations: UriSkosRelation[]
}>()

const emit = defineEmits<{
  (e: 'update:uriRelations', v: UriSkosRelation[]): void
}>()

/** État local */
const rows = ref<UriSkosRelation[]>([...props.uriRelations])
const { t } = useI18n()

/** Sync props -> local */
watch(
  () => props.uriRelations,
  (nv) => { rows.value = [...nv] },
  { deep: true }
)

/** Helper : émettre un NOUVEL array (immutabilité) */
function pushChange () {
  emit('update:uriRelations', rows.value.map(r => ({ ...r })))
}

/** Actions */
function removeRow (row: UriSkosRelation) {
  rows.value = rows.value.filter(r => !(r.relationDtoKey === row.relationDtoKey && r.uri === row.uri))
  pushChange()
}

function updateRelation (row: UriSkosRelation, newKey: string) {
  const idx = rows.value.findIndex(r => r.uri === row.uri && r.relationDtoKey === row.relationDtoKey)
  if (idx >= 0) {
    rows.value[idx] = { ...rows.value[idx], relationDtoKey: newKey }
    pushChange()
  }
}

/** Colonnes (Naive UI) */
const columns = [
  {
    title: t('component.skos.relation'),
    key: 'relation',
    sorter: (a: UriSkosRelation, b: UriSkosRelation) =>
      a.relationDtoKey.localeCompare(b.relationDtoKey),
    render: (row: UriSkosRelation) =>
      h(SkosSelector, {
        labelAsCurrentSelectedRelation: true,
        selectedRelation: row.relationDtoKey,
        // v-model:selectedRelation en style composition
        'onUpdate:selectedRelation': (val: string) => updateRelation(row, val)
      })
  },
  {
    title: t('component.skos.uri'),
    key: 'relationURI',
    render: (row: UriSkosRelation) =>
      h('a', { href: row.uri, target: '_blank', rel: 'noopener noreferrer' }, row.uri)
  },
  {
    title: t('component.common.actions'),
    key: 'actions',
    width: 100,
    align: 'center' as const,
    render: (row: UriSkosRelation) =>
      h(
        NButton,
        { size: 'small', type: 'error', onClick: () => removeRow(row) },
        { default: () => h('i', { class: 'bi bi-trash3' }) }
      )
  }
]
</script>

<style scoped>
/* .v-step-skos-relation-table {
} */
</style>
