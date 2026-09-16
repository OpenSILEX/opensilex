<template>
  <InfiteScrollDropdown
    v-model:selected="internalValue"
    :fetchPage="searchParents"
    :itemLoadingMethod="getParentsByURI"
    :conversionMethod="scientificObjectToSelectOption"
    :label="property?.name ?? property?.uri"
    :multiple="property?.is_list"
    :required="property?.is_required"
    :placeholder="t('ScientificObjectParentPropertySelector.parent-placeholder')"
  />
</template>

<script setup lang="ts">
import { computed, inject } from 'vue'
import type { SelectOption } from 'naive-ui'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type { ScientificObjectsService } from 'opensilex-core/api/scientificObjects.service'
import type { VueRDFTypePropertyDTO } from '@/lib'
import { useI18n } from 'vue-i18n'
import InfiteScrollDropdown from "@/components/common/forms/InfiteScrollDropdown.vue";


const props = withDefaults(defineProps<{
  property: VueRDFTypePropertyDTO
  value?: string | string[]
  context?: { experimentURI?: string } | string
  excluded?: Set<string>
}>(), {
  excluded: () => new Set<string>()
})

const emit = defineEmits<{
  (e: 'update:value', value: string | string[] | undefined): void
}>()

const { t } = useI18n()
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!

const service = opensilex.getService<ScientificObjectsService>(
  'opensilex.ScientificObjectsService'
)

const internalValue = computed({
  get() {
    if (props.property?.is_list) {
      return Array.isArray(props.value) ? props.value : props.value ? [props.value] : []
    }

    return Array.isArray(props.value) ? props.value[0] : props.value
  },
  set(value: string | string[] | undefined) {
    if (props.property?.is_list) {
      emit('update:value', Array.isArray(value) ? value : value ? [value] : [])
    } else {
      emit('update:value', Array.isArray(value) ? value[0] : value)
    }
  }
})

function getExperimentURI() {
  if (!props.context) {
    return undefined
  }

  if (typeof props.context === 'string') {
    return props.context
  }

  return props.context.experimentURI
}

function getSearchTypes() {
  if (!props.property?.target_property) {
    return undefined
  }

  if (
    opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI &&
    opensilex.compareUris(
      props.property.target_property,
      opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI
    )
  ) {
    return undefined
  }

  return [props.property.target_property]
}

const scientificObjectToSelectOption = (so: any): SelectOption => ({
  label: `${so.name ?? so.uri} (${so.rdf_type_name ?? so.rdf_type ?? ''})`,
  value: so.uri
})

/** Loads one page of results. `page` is zero-based. */
async function searchParents(query: string, page: number, pageSize: number) {
  const types = getSearchTypes()

  const http: any = await service.searchScientificObjects(
    getExperimentURI(),
    types,
    query,
    undefined,       // parent
    undefined,       // germplasms
    undefined,       // factor_levels
    undefined,       // facility
    undefined,       // variables
    undefined,       // devices
    undefined,       // existence_date
    undefined,       // creation_date
    undefined,       // criteria_on_data
    ['name=asc'],    // order_by
    page,
    pageSize
  )

  // The object being edited (and anything else the caller excludes) must not be selectable as its
  // own parent.
  http.response.result = http.response.result
    .filter((so: any) => !props.excluded?.has(so.uri))

  return http
}

/** Loads the already selected elements (update form), so that their name can be displayed. */
async function getParentsByURI(soURIs: string[] | string) {
  const uris = Array.isArray(soURIs)
    ? soURIs
    : soURIs
      ? [soURIs]
      : []

  if (uris.length === 0) {
    return []
  }

  const http: any = await service.getScientificObjectsListByUris(
    getExperimentURI(),
    uris
  )

  return http.response.result
}
</script>

<i18n>
en:
  ScientificObjectParentPropertySelector:
    label: Facilities
    placeholder: Select a facility
    parent-placeholder: Select a scientific object

fr:
  ScientificObjectParentPropertySelector:
    label: Installation environnementale
    placeholder: Sélectionner une installation environnementale
    parent-placeholder: Sélectionner un objet scientifique
</i18n>