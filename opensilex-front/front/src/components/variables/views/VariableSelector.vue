<template>
  <InfiteScrollDropdown
    v-model:selected="variablesURI"
    :fetchPage="searchVariables"
    :itemLoadingMethod="load"
    :conversionMethod="variableToSelectOption"
    :label="label"
    :multiple="multiple"
    :placeholder="placeholder"
    :noResultsText="t('VariableSelector.filter-search-no-result')"
    :required="required"
    @selectionChange="(option) => emit('selectionChange', option)"
    @clear="emit('clear')"
  />
</template>

<script setup lang="ts">
import { computed, inject } from 'vue'
import type { SelectOption } from 'naive-ui'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type HttpResponse from 'opensilex-security/HttpResponse'
import type { OpenSilexResponse } from 'opensilex-security/HttpResponse'
import type { NamedResourceDTO, VariableDetailsDTO } from 'opensilex-core/index'
import type { VariablesService } from 'opensilex-core/api/variables.service'
import { useI18n } from 'vue-i18n'
import InfiteScrollDropdown from "@/components/common/forms/InfiteScrollDropdown.vue";

const props = withDefaults(defineProps<{
  variables?: string | string[]
  label?: string
  multiple?: boolean
  required?: boolean
}>(), {
  required: false
})

const emit = defineEmits<{
  (e: 'update:variables', value: string | string[] | undefined): void
  (e: 'selectionChange', option: SelectOption | SelectOption[] | undefined): void
  (e: 'clear'): void
}>()

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const { t } = useI18n()

const service = $opensilex.getService<VariablesService>('opensilex.VariablesService')

// v-model proxy
const variablesURI = computed({
  get: () => props.variables,
  set: (value) => emit('update:variables', value)
})

const placeholder = computed(() => {
  return props.multiple
    ? t('VariableSelector.placeholder-multiple')
    : t('VariableSelector.placeholder')
})

/** Loads one page of results. `page` is zero-based. */
function searchVariables(query: string, page: number, pageSize: number) {
  // The treeselect used to send ".*" to mean "everything"; the API expects no name filter for that.
  const name = (!query || query === '.*') ? undefined : query

  return service.searchVariables(
    name, // name
    undefined, // entity
    undefined, // entity of interest
    undefined, // characteristic
    undefined, // method
    undefined, // unit
    undefined, // included group
    undefined, // not included group
    undefined, // datatype
    undefined, // time interval
    undefined, // species
    undefined, // with associated data
    undefined, // experiment
    undefined, // object
    undefined, // devices
    ['name=asc'],
    page,
    pageSize
  ).catch($opensilex.errorHandler)
}

/** Loads the already selected elements (update form), so that their name can be displayed. */
async function load(variables: string[]) {
  try {
    const http: HttpResponse<OpenSilexResponse<VariableDetailsDTO[]>> =
      await service.getVariablesByURIs(variables)

    return http?.response?.result
  } catch (error) {
    $opensilex.errorHandler(error)
    return undefined
  }
}

const variableToSelectOption = (dto: NamedResourceDTO): SelectOption => ({
  label: dto.name,
  value: dto.uri
})
</script>

<style scoped>
</style>

<i18n>
en:
  VariableSelector:
    placeholder: Select a variable
    placeholder-multiple: Select one or more variables
    filter-search-no-result: No variable found

fr:
  VariableSelector:
    placeholder: Sélectionner une variable
    filter-search-no-result: Aucune variable trouvée
    placeholder-multiple: Sélectionner une ou plusieurs variables
</i18n>