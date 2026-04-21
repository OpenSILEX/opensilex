<template>
  <opensilex-FormSelector
    :key="locale"
    :label="label"
    v-model:selected="typesURI"
    :multiple="multiple"
    :optionsLoadingMethod="loadTypes"
    :placeholder="placeholder"
    @select="onSelect"
    @deselect="onDeselect"
    @enterKey="onEnter"
  />
</template>

<script setup lang="ts">
import { inject, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type HttpResponse from 'opensilex-security/HttpResponse'
import type { OpenSilexResponse } from 'opensilex-security/HttpResponse'
import type { ListItemDTO } from 'opensilex-core'
import type { ScientificObjectsService } from 'opensilex-core/api/scientificObjects.service'

const props = defineProps<{
  selected?: string | string[]
  experimentURI?: string
  label?: string
  multiple?: boolean
  placeholder?: string
}>()

const emit = defineEmits<{
  (e: 'update:selected', value: string | string[] | undefined): void
  (e: 'select', value: string): void
  (e: 'deselect', value: string): void
  (e: 'handlingEnterKey'): void
}>()

const $opensilex = inject<OpenSilexVuePlugin>('opensilex')!
const { t, locale } = useI18n()

const placeholder = computed(
  () => props.placeholder || t('ScientificObjectTypeSelector.placeholder')
)

const typesURI = computed({
  get: () => props.selected,
  set: (value) => emit('update:selected', value)
})

const loadTypes = async (): Promise<ListItemDTO[]> => {
  const service = $opensilex.getService<ScientificObjectsService>(
    'opensilex.ScientificObjectsService'
  )

  const hasExperiment =
    props.experimentURI !== null &&
    props.experimentURI !== undefined &&
    props.experimentURI !== ''

  const http: HttpResponse<OpenSilexResponse<ListItemDTO[]>> = hasExperiment
    ? await service.getUsedTypes(props.experimentURI)
    : await service.getUsedTypes()

  return http.response.result ?? []
}

const onSelect = (value: string) => {
  emit('select', value)
}

const onDeselect = (value: string) => {
  emit('deselect', value)
}

const onEnter = () => {
  emit('handlingEnterKey')
}
</script>

<style scoped>
</style>

<i18n>
en:
  ScientificObjectTypeSelector:
    placeholder: Select a type

fr:
  ScientificObjectTypeSelector:
    placeholder: Sélectionner un type
</i18n>