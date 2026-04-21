<template>
  <opensilex-FormSelector
    ref="formSelector"
    :label="label"
    v-model:selected="variablesURI"
    :multiple="multiple"
    :searchMethod="searchVariables"
    :itemLoadingMethod="load"
    :conversionMethod="variableToSelectNode"
    :clearable="clearable"
    :placeholder="placeholder"
    :required="required"
    :defaultSelectedValue="defaultSelectedValue"
    noResultsText="VariableSelector.filter-search-no-result"
    @clear="emit('clear')"
    @select="select"
    @deselect="deselect"
  />
</template>

<script setup lang="ts">
import { computed, inject, ref } from 'vue'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type HttpResponse from 'opensilex-security/HttpResponse'
import type { OpenSilexResponse } from 'opensilex-security/HttpResponse'
import type { NamedResourceDTO, VariableDetailsDTO } from 'opensilex-core/index'
import type { VariablesService } from 'opensilex-core/api/variables.service'
import { useI18n } from 'vue-i18n'

const props = withDefaults(defineProps<{
  variables?: string | string[]
  label?: string
  defaultSelectedValue?: string
  multiple?: boolean
  required?: boolean
  clearable?: boolean
}>(), {
  required: false
})

const emit = defineEmits<{
  (e: 'update:variables', value: string | string[] | undefined): void
  (e: 'clear'): void
  (e: 'select', value: any): void
  (e: 'deselect', value: any): void
}>()

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const formSelector = ref<any>(null)
const { t } = useI18n()

const service = $opensilex.getService<VariablesService>('opensilex.VariablesService')
const filterLabel = ref<string | undefined>('')

const variablesURI = computed({
  get: () => props.variables,
  set: (value) => emit('update:variables', value)
})

const placeholder = computed(() => {
  return props.multiple
    ? 'VariableSelector.placeholder-multiple'
    : t('VariableSelector.placeholder')
})

function searchVariables(query: string, page: number, pageSize: number) {
  filterLabel.value = query

  if (filterLabel.value === '.*') {
    filterLabel.value = undefined
  }

  return service.searchVariables(
    filterLabel.value, // name
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

function variableToSelectNode(dto: NamedResourceDTO) {
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