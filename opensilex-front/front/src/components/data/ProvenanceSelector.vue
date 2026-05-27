<template>
  <opensilex-FormSelector
    ref="formSelector"
    :label="label"
    v-model:selected="provenancesURI"
    :multiple="multiple"
    :searchMethod="searchProvenances"
    :itemLoadingMethod="loadProvenances"
    :conversionMethod="provenancesToSelectNode"
    :placeholder="
      multiple
        ? t('component.data.form.selector.placeholder-multiple')
        : t('component.data.form.selector.placeholder')
    "
    noResultsText="component.data.form.selector.filter-search-no-result"
    @clear="emit('clear')"
    @select="select"
    @deselect="deselect"
    @loadMoreItems="loadMoreItems"
    :disableBranchNodes="true"
    :showCount="true"
    :actionHandler="actionHandler"
    :viewHandler="viewHandler"
    :required="required"
    :viewHandlerDetailsVisible="viewHandlerDetailsVisible"
  />
</template>

<script setup lang="ts">
import { computed, inject, nextTick, ref } from 'vue'

import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import { useI18n } from 'vue-i18n'
import type HttpResponse from 'opensilex-core/HttpResponse'
import type {
  OpenSilexResponse,
  ProvenanceGetDTO
} from 'opensilex-core/index'

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
  (e: 'clear'): void
  (e: 'select', value: any): void
  (e: 'deselect', value: any): void
}>()

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const { t } = useI18n()

const formSelector = ref<any>(null)

const pageSize = ref(10)
const filterLabel = ref<string | undefined>(undefined)

const provenancesURI = computed({
  get() {
    return props.provenances
  },
  set(value: any) {
    emit('update:provenances', value)
  }
})

function refresh() {
  formSelector.value?.refresh?.()
}

function loadProvenances(provenancesURI: any) {
  return $opensilex
    .getService('opensilex.DataService')
    .getProvenancesByURIs(provenancesURI)
    .then(
      (http: HttpResponse<OpenSilexResponse<Array<ProvenanceGetDTO>>>) =>
        http.response.result
    )
}

function searchProvenances(label: string, page: number, requestedPageSize: number) {
  filterLabel.value = label

  if (filterLabel.value === '.*') {
    filterLabel.value = undefined
  }

  return $opensilex
    .getService('opensilex.DataService')
    .searchProvenance(
      filterLabel.value,
      undefined,
      undefined,
      undefined,
      undefined,
      undefined,
      undefined,
      undefined,
      pageSize.value
    )
    .then(
      (http: HttpResponse<OpenSilexResponse<Array<ProvenanceGetDTO>>>) => http
    )
}

function provenancesToSelectNode(dto: ProvenanceGetDTO) {
  return {
    id: dto.uri,
    label: dto.name
  }
}

function select(value: any) {
  emit('select', value)
}

function deselect(value: any) {
  emit('deselect', value)
}

function loadMoreItems() {
  pageSize.value = 0
  formSelector.value?.refresh?.()

  nextTick(() => {
    formSelector.value?.openTreeselect?.()
  })
}

defineExpose({
  refresh,
  loadProvenances,
  searchProvenances,
  provenancesToSelectNode,
  select,
  deselect,
  loadMoreItems
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