<template>
  <InfiteScrollDropdown
    v-model:selected="provenancesURI"
    :fetchPage="searchProvenances"
    :itemLoadingMethod="loadProvenances"
    :conversionMethod="provenanceToSelectOption"
    :path="path"
    :label="label"
    :multiple="multiple"
    :required="required"
    :actionHandler="actionHandler"
    :viewHandler="viewHandler"
    :viewHandlerDetailsVisible="viewHandlerDetailsVisible"
    :placeholder="resolvedPlaceholder"
    :noResultsText="t('component.data.form.selector.filter-search-no-result')"
    @selectionChange="(option) => emit('selectionChange', option)"
    @clear="emit('clear')"
  />
</template>

<script setup lang="ts">
import { computed, inject } from 'vue'
import { useI18n } from 'vue-i18n'
import type { SelectOption } from 'naive-ui'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type HttpResponse from 'opensilex-core/HttpResponse'
import type {
  OpenSilexResponse,
  ProvenanceGetDTO
} from 'opensilex-core/index'
import InfiteScrollDropdown from '@/components/common/forms/InfiteScrollDropdown.vue'

const props = withDefaults(defineProps<{
  provenances?: any
  label?: string
  required?: boolean
  experiment?: string
  multiple?: boolean
  actionHandler?: Function
  viewHandler?: Function
  viewHandlerDetailsVisible?: boolean
  scientificObject?: string
  device?: string
  /** Path of the field in the parent NForm model, used for validation */
  path?: string
}>(), {
  provenances: undefined,
  label: 'component.data.provenance.search',
  required: false,
  experiment: undefined,
  multiple: false,
  actionHandler: undefined,
  viewHandler: undefined,
  viewHandlerDetailsVisible: false,
  scientificObject: undefined,
  device: undefined
})

const emit = defineEmits<{
  (e: 'update:provenances', value: any): void
  (e: 'selectionChange', option: SelectOption | SelectOption[] | undefined): void
  (e: 'clear'): void
}>()

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const { t } = useI18n()

// v-model proxy
const provenancesURI = computed({
  get() {
    return props.provenances
  },
  set(value: any) {
    emit('update:provenances', value)
  }
})

const resolvedPlaceholder = computed(() =>
  props.multiple
    ? t('component.data.form.selector.placeholder-multiple')
    : t('component.data.form.selector.placeholder')
)

/** Loads the already selected elements (update form), so that their name can be displayed. */
function loadProvenances(uris: string[]): Promise<ProvenanceGetDTO[]> {
  return $opensilex
    .getService('opensilex.DataService')
    .getProvenancesByURIs(uris)
    .then(
      (http: HttpResponse<OpenSilexResponse<Array<ProvenanceGetDTO>>>) =>
        http.response.result
    )
}

/** Loads one page of results. `page` is zero-based. */
function searchProvenances(label: string, page: number, pageSize: number) {
  // The treeselect used to send ".*" to mean "everything"; the API expects no name filter for that.
  const name = (!label || label === '.*') ? undefined : label

  return $opensilex
    .getService('opensilex.DataService')
    .searchProvenance(
      name,
      undefined,
      undefined,
      undefined,
      undefined,
      undefined,
      ['name=asc'],
      page,
      pageSize
    )
    .then(
      (http: HttpResponse<OpenSilexResponse<Array<ProvenanceGetDTO>>>) => http
    )
}

const provenanceToSelectOption = (dto: ProvenanceGetDTO): SelectOption => ({
  label: dto.name,
  value: dto.uri
})
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  component:
    data:
      form:
        selector:
          placeholder: Select a provenance
          placeholder-multiple: Select one or more provenance(s)
          filter-search-no-result: No provenance found

fr:
  component:
    data:
      form:
        selector:
          placeholder: Sélectionner une provenance
          placeholder-multiple: Sélectionner une ou plusieurs provenance(s)
          filter-search-no-result: Aucune provenance trouvée
</i18n>