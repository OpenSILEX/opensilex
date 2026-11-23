<template>
  <InfiniteScrollDropdown
    v-model:selected="provenancesURI"
    :fetchPage="searchProvenances"
    :itemLoadingMethod="loadProvenances"
    :conversionMethod="provenanceToSelectOption"
    :resultLimit="0"
    :label="label"
    :multiple="multiple"
    :required="required"
    :actionHandler="actionHandler"
    :viewHandler="viewHandler"
    :viewHandlerDetailsVisible="viewHandlerDetailsVisible"
    :placeholder="resolvedPlaceholder"
    :noResultsText="t('component.datafile.provenance-selector.filter-search-no-result')"
    @selectionChange="(option) => emit('selectionChange', option)"
    @clear="emit('clear')"
    @handlingEnterKey="emit('handlingEnterKey')"
  />
</template>

<script setup lang="ts">
import { computed, inject } from 'vue'
import { useI18n } from 'vue-i18n'
import type { SelectOption } from 'naive-ui'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import HttpResponse, { MetadataDTO, OpenSilexResponse, PaginationDTO } from '@/lib/HttpResponse'
import type { ProvenanceGetDTO } from 'opensilex-core/index'
import type { DataService } from 'opensilex-core/api/data.service'
import InfiniteScrollDropdown from '@/components/common/forms/InfiniteScrollDropdown.vue'

//#region Public
const emit = defineEmits<{
  selectionChange: [option: SelectOption | SelectOption[] | undefined]
  clear: []
  handlingEnterKey: []
}>()

const props = withDefaults(defineProps<{
  /** Lists the provenances used by data (observations), or by datafiles: each one has its own API */
  source: 'data' | 'datafiles'
  label?: string
  placeholder?: string
  required?: boolean
  multiple?: boolean
  /** Only the provenances used in these experiments are listed */
  experiments?: string[]
  /** Only the provenances used on these targets are listed */
  targets?: string[]
  /** Only the provenances used by these devices are listed */
  devices?: string[]
  /** Only the provenances used for these variables are listed. Data only: datafiles have no variable */
  variables?: string[]
  actionHandler?: () => void
  viewHandler?: () => void
  viewHandlerDetailsVisible?: boolean
}>(), {
  label: 'component.data.provenance.search',
  required: false,
  multiple: false,
  viewHandlerDetailsVisible: false
})

const provenancesURI = defineModel<string | string[] | null>('provenances')
//#endregion

//#region Private

//#region Plugins & services
const { t } = useI18n()
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const dataService = $opensilex.getService<DataService>('opensilex.DataService')
//#endregion

//#region Data & Computed
const resolvedPlaceholder = computed(() => {
  if (props.placeholder) {
    return props.placeholder
  }
  return props.multiple
    ? t('component.datafile.provenance-selector.placeholder-multiple')
    : t('component.datafile.provenance-selector.placeholder')
})
//#endregion

//#region Methods
/** Every provenance used in the current context, from the API of the `source` */
function fetchUsedProvenances(): Promise<HttpResponse<OpenSilexResponse<ProvenanceGetDTO[]>>> {
  return props.source === 'data'
    ? dataService.getUsedProvenancesByTargets(props.experiments, props.variables, props.devices, props.targets)
    : dataService.getDatafilesProvenancesByTargets(props.experiments, props.devices, props.targets)
}

/**
 * Both APIs return every used provenance at once, without name filter nor pagination: the name filter is
 * applied here, and the whole result is returned as a single page (`resultLimit` 0).
 */
async function searchProvenances(name: string): Promise<HttpResponse<OpenSilexResponse<ProvenanceGetDTO[]>>> {
  const http = await fetchUsedProvenances()
  const searchedName = name.trim().toLowerCase()
  const matching = (http.response.result ?? [])
    .filter(provenance => provenance.name.toLowerCase().includes(searchedName))
    .sort((a, b) => a.name.localeCompare(b.name))

  if (searchedName === '') {
    dropUnavailableSelection(matching)
  }

  const pagination = new PaginationDTO(0, 0, matching.length, 1, matching.length, false)
  const metadata = new MetadataDTO(pagination, http.response.metadata?.status ?? [], [])
  return new HttpResponse(new OpenSilexResponse(matching, metadata), http.status, http.headers)
}

/**
 * The available provenances depend on the experiments, targets and devices: once they change, a provenance
 * selected before may no longer be part of them, and is then unselected.
 */
function dropUnavailableSelection(availableProvenances: ProvenanceGetDTO[]) {
  const availableUris = new Set(availableProvenances.map(provenance => $opensilex.getShortUri(provenance.uri ?? '')))
  const isAvailable = (uri: string) => availableUris.has($opensilex.getShortUri(uri))
  const selected = provenancesURI.value

  if (Array.isArray(selected)) {
    const stillAvailable = selected.filter(isAvailable)
    if (stillAvailable.length !== selected.length) {
      provenancesURI.value = stillAvailable
    }
  } else if (selected && !isAvailable(selected)) {
    provenancesURI.value = undefined
  }
}

/** Loads the already selected elements (e.g. a filter restored from the URL), so that their name can be displayed. */
async function loadProvenances(uris: string[]): Promise<ProvenanceGetDTO[]> {
  const http = await dataService.getProvenancesByURIs(uris)
  return http.response.result ?? []
}

function provenanceToSelectOption(provenance: ProvenanceGetDTO): SelectOption {
  return { label: provenance.name, value: provenance.uri }
}
//#endregion

//#endregion
</script>
