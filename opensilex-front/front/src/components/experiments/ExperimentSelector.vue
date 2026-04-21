<template>
  <opensilex-FormSelector
    ref="experimentSelector"
    :required="required"
    :label="label"
    v-model:selected="experimentsURI"
    :multiple="multiple"
    :searchMethod="searchExperiments"
    :placeholder="placeholder"
    :noResultsText="t('component.experiment.form.selector.filter-search-no-result')"
    @clear="emit('clear')"
    @select="select"
    @deselect="deselect"
    @keyup.enter="onEnter"
  />
</template>

<script setup lang="ts">
import { computed, inject, ref } from 'vue'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type HttpResponse from 'opensilex-security/HttpResponse'
import type { OpenSilexResponse } from 'opensilex-security/HttpResponse'
import type { ExperimentGetListDTO } from 'opensilex-core/index'
import { useI18n } from 'vue-i18n'

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const { t } = useI18n()

const emit = defineEmits<{
  (e: 'update:experiments', value: any): void
  (e: 'select', value: any): void
  (e: 'deselect', value: any): void
  (e: 'clear'): void
  (e: 'handlingEnterKey'): void
}>()

const props = withDefaults(defineProps<{
  experiments?: any
  label?: string
  multiple?: boolean
  required?: boolean
}>(), {
  label: 'component.experiment.experiment',
  multiple: false,
  required: false
})

const experimentSelector = ref<any>(null)
const pageSize = ref(10)
const page = ref(0)

const experimentsByUriCache = ref<Map<string, ExperimentGetListDTO>>(new Map())

const experimentsURI = computed({
  get: () => props.experiments,
  set: (value) => emit('update:experiments', value)
})

const placeholder = computed(() => {
  return props.multiple
    ? t('component.experiment.form.selector.placeholder-multiple')
    : t('component.experiment.form.selector.placeholder')
})

function searchExperiments(name: string, page: number, pageSize: number) {
  return $opensilex
    .getService('opensilex.ExperimentsService')
    .searchExperiments(
      name,
      undefined,
      undefined,
      undefined,
      undefined,
      undefined,
      undefined,
      undefined,
      undefined,
      undefined,
      page,
      pageSize
    )
    .then((http: HttpResponse<OpenSilexResponse<Array<ExperimentGetListDTO>>>) => {
      if (http?.response?.result) {
        experimentsByUriCache.value.clear()
        http.response.result.forEach((dto) => {
          experimentsByUriCache.value.set(dto.uri, dto)
        })
      }
      return http
    })
}

function select(value: any) {
  emit('select', value)
}

function deselect(value: any) {
  emit('deselect', value)
}

function onEnter() {
  emit('handlingEnterKey')
}

function loadMoreItems() {
  pageSize.value = 0
  experimentSelector.value?.refresh?.()
  Promise.resolve().then(() => {
    experimentSelector.value?.openTreeselect?.()
  })
}

defineExpose({
  loadMoreItems
})
</script>

<style scoped>
</style>

<i18n>
en:
  component:
    experiment:
      form:
        selector:
          placeholder: Select one experiment
          placeholder-multiple: Select one or more Experiments
          filter-search-no-result: No experiment found

fr:
  component:
    experiment:
      form:
        selector:
          placeholder: Sélectionner une expérimentation
          placeholder-multiple: Sélectionner une ou plusieurs expérimentations
          filter-search-no-result: Aucune expérimentation trouvée
</i18n>