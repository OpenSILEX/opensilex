<template>
  <InfiniteScrollDropdown
    v-model:selected="germplasmURI"
    :fetchPage="search"
    :itemLoadingMethod="load"
    :conversionMethod="germplasmToSelectOption"
    :label="computedLabel"
    :multiple="multiple"
    :required="required"
    :placeholder="computedPlaceholder"
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
import type { GermplasmService } from 'opensilex-core/api/germplasm.service'
import type HttpResponse from 'opensilex-security/HttpResponse'
import type { OpenSilexResponse } from 'opensilex-security/HttpResponse'
import type { GermplasmGetAllDTO } from 'opensilex-core/model/germplasmGetAllDTO'
import InfiniteScrollDropdown from "@/components/common/forms/InfiniteScrollDropdown.vue";

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const { t } = useI18n()

const germplasmURI = defineModel<string[] | undefined>('germplasm')

const emit = defineEmits<{
  (e: 'selectionChange', option: SelectOption | SelectOption[] | undefined): void
  (e: 'clear'): void
  (e: 'handlingEnterKey'): void
}>()

const props = withDefaults(defineProps<{
  label?: string
  placeholder?: string
  required?: boolean
  multiple?: boolean
  experiment?: string
}>(), {
  label: 'GermplasmSelector.label',
  placeholder: 'GermplasmSelector.placeholder',
  required: false,
  multiple: false
})

const germplasmService = $opensilex.getService<GermplasmService>('opensilex.GermplasmService')

const computedLabel = computed(() => t(props.label))
const computedPlaceholder = computed(() => t(props.placeholder))

/** Loads one page of results. `page` is zero-based. */
function search(query: string, page: number, pageSize: number) {
  return germplasmService.searchGermplasm(
    undefined, // uri
    undefined, // type
    query, // name
    undefined, // code
    undefined, // productionYear
    undefined, // species
    undefined, // variety
    undefined, // accession
    undefined, // GermplasmGroup
    undefined, // institute
    props.experiment || undefined, // experiment
    [],
    [],
    [],
    undefined, // metadata
    undefined, // is_public
    ['name=asc'], // orderBy
    page,
    pageSize
  )
}

/** Loads the already selected elements (update form), so that their name can be displayed. */
async function load(uris: string[]): Promise<GermplasmGetAllDTO[]> {
  try {
    const http: HttpResponse<OpenSilexResponse<GermplasmGetAllDTO[]>> =
      await germplasmService.getGermplasmsByURI(uris)

    return http.response.result ?? []
  } catch (error) {
    $opensilex.errorHandler(error)
    return []
  }
}

const germplasmToSelectOption = (germplasm: GermplasmGetAllDTO): SelectOption => {
  let label = `${germplasm.name} (${germplasm.rdf_type_name}`

  if (germplasm.species != null) {
    label += ` - ${germplasm.species_name}`
  }

  label += ')'

  return {
    label,
    value: germplasm.uri
  }
}
</script>

<style scoped>
</style>

<i18n>
en:
  GermplasmSelector:
    label: Germplasm
    placeholder: Select a germplasm

fr:
  GermplasmSelector:
    label: Ressources Génétiques
    placeholder: Sélectionner une resource génétique
</i18n>