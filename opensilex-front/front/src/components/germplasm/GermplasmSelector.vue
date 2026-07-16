<template>
  <opensilex-FormSelector
    ref="formSelector"
    :label="computedLabel"
    v-model:selected="germplasmURI"
    :multiple="multiple"
    :required="required"
    :searchMethod="search"
    :itemLoadingMethod="load"
    :conversionMethod="convertGermplasmDTO"
    :placeholder="computedPlaceholder"
    @clear="emit('clear')"
    @select="select"
    @deselect="deselect"
    @enterKey="onEnter"
  />
</template>

<script setup lang="ts">
import { computed, inject, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type { GermplasmService } from 'opensilex-core/api/germplasm.service'
import type HttpResponse from 'opensilex-security/HttpResponse'
import type { OpenSilexResponse } from 'opensilex-security/HttpResponse'
import type { GermplasmGetAllDTO } from 'opensilex-core/model/germplasmGetAllDTO'

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const { t, locale } = useI18n()

const emit = defineEmits<{
  (e: 'update:germplasm', value: string[] | undefined): void
  (e: 'select', value: any): void
  (e: 'deselect', value: any): void
  (e: 'clear'): void
  (e: 'handlingEnterKey'): void
}>()

const props = withDefaults(defineProps<{
  germplasm?: string[]
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

const formSelector = ref<any>(null)

const germplasmService = $opensilex.getService<GermplasmService>('opensilex.GermplasmService')

const germplasmURI = computed({
  get: () => props.germplasm,
  set: (value) => emit('update:germplasm', value)
})

const computedLabel = computed(() => t(props.label))
const computedPlaceholder = computed(() => t(props.placeholder))

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
    [], // orderBy
    page,
    pageSize
  )
}

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

function convertGermplasmDTO(germplasm: GermplasmGetAllDTO) {
  let label = `${germplasm.name} (${germplasm.rdf_type_name}`

  if (germplasm.species != null) {
    label += ` - ${germplasm.species_name}`
  }

  label += ')'

  return {
    id: germplasm.uri,
    label
  }
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

defineExpose({
  formSelector
})
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