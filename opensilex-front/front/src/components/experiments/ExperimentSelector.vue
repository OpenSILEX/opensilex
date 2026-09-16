<template>
  <InfiteScrollDropdown
    v-model:selected="experimentsURI"
    :fetchPage="searchExperiments"
    :itemLoadingMethod="loadExperiments"
    :conversionMethod="experimentToSelectOption"
    :path="path"
    :required="required"
    :label="label"
    :multiple="multiple"
    :placeholder="placeholder"
    @selectionChange="(option) => emit('selectionChange', option)"
    @clear="emit('clear')"
    @handlingEnterKey="emit('handlingEnterKey')"
  />
</template>

<script setup lang="ts">
import { computed, inject } from 'vue'
import type { SelectOption } from 'naive-ui'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type HttpResponse from 'opensilex-security/HttpResponse'
import type { OpenSilexResponse } from 'opensilex-security/HttpResponse'
import type { ExperimentGetListDTO } from 'opensilex-core/index'
import { useI18n } from 'vue-i18n'
import InfiteScrollDropdown from "@/components/common/forms/InfiteScrollDropdown.vue";

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const { t } = useI18n()

const emit = defineEmits<{
  (e: 'update:experiments', value: any): void
  (e: 'selectionChange', option: SelectOption | SelectOption[] | undefined): void
  (e: 'clear'): void
  (e: 'handlingEnterKey'): void
}>()

const props = withDefaults(defineProps<{
  experiments?: any
  label?: string
  multiple?: boolean
  required?: boolean
  /** Path of the field in the parent NForm model, used for validation */
  path?: string
}>(), {
  label: 'component.experiment.experiment',
  multiple: false,
  required: false
})

// v-model proxy
const experimentsURI = computed({
  get: () => props.experiments,
  set: (value) => emit('update:experiments', value)
})

const placeholder = computed(() => {
  return props.multiple
    ? t('component.experiment.form.selector.placeholder-multiple')
    : t('component.experiment.form.selector.placeholder')
})

/** Loads one page of results. `page` is zero-based. */
function searchExperiments(name: string, page: number, pageSize: number) {
  return $opensilex
    .getService('opensilex.ExperimentsService')
    .searchExperiments(
      name,
      undefined,       // year
      undefined,       // is_ended
      undefined,       // species
      undefined,       // factors
      undefined,       // projects
      undefined,       // is_public
      undefined,       // facilities
      undefined,       // funding
      ['name=asc'],    // order_by
      page,
      pageSize
    )
    .then((http: HttpResponse<OpenSilexResponse<Array<ExperimentGetListDTO>>>) => http)
}

/**
 * Loads the already selected elements, so that their name can be displayed. Needed whenever an
 * experiment arrives without a label: an update form, or a filter restored from the URL.
 */
function loadExperiments(uris: string[]) {
  return $opensilex
    .getService('opensilex.ExperimentsService')
    .getExperimentsByURIs(uris)
    .then((http: HttpResponse<OpenSilexResponse<Array<ExperimentGetListDTO>>>) => http.response.result)
    .catch($opensilex.errorHandler)
}

const experimentToSelectOption = (dto: ExperimentGetListDTO): SelectOption => ({
  label: dto.name,
  value: dto.uri
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