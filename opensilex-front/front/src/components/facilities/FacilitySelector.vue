<template>
  <div>
    <opensilex-FormSelector
      :label="label"
      v-model:selected="facilitiesURI"
      :multiple="multiple"
      :helpMessage="helpMessage"
      :placeholder="t(placeholder)"
      :searchMethod="searchFacilities"
      :itemLoadingMethod="loadFacilities"
      :conversionMethod="facilityToSelectNode"
      noResultsText="FacilitySelector.no-result"
      @select="(v) => emit('select', v)"
      @deselect="(v) => emit('deselect', v)"
      @clear="onClear"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, inject, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type { OrganizationsService } from 'opensilex-core/api/organizations.service'
import type { NamedResourceDTO } from 'opensilex-core/index'
import type HttpResponse from 'opensilex-core/HttpResponse'
import type { OpenSilexResponse } from 'opensilex-core/HttpResponse'

const { t } = useI18n()

const props = withDefaults(
  defineProps<{
    facilities?: any // string | string[] selon multiple
    label?: string
    multiple?: boolean
    helpMessage?: string
    placeholder?: string
    required?: boolean
  }>(),
  {
    facilities: () => [],
    placeholder: 'FacilitySelector.placeholder',
    multiple: false,
    required: false
  }
)

const emit = defineEmits<{
  (e: 'update:facilities', v: any): void
  (e: 'select', v: any): void
  (e: 'deselect', v: any): void
  (e: 'clear'): void
}>()

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!

const service = $opensilex.getService<OrganizationsService>('opensilex.OrganizationsService')

// v-model replacement of PropSync("facilities")
const facilitiesURI = ref<any>(props.facilities)

watch(
  () => props.facilities,
  (v) => {
    facilitiesURI.value = v
  },
  { deep: true }
)

watch(
  facilitiesURI,
  (v) => emit('update:facilities', v),
  { deep: true }
)

async function searchFacilities(searchQuery: string) {
  try {
    const http = await service.minimalSearchFacilities(
      searchQuery,
      undefined,
      undefined,
      undefined,
      0
    ) as HttpResponse<OpenSilexResponse<NamedResourceDTO[]>>
    return http
  } catch (e) {
    $opensilex.errorHandler(e as any)
    throw e
  }
}

async function loadFacilities(facilitiesUris: string[]) {
  if (!facilitiesUris || facilitiesUris.length === 0) return undefined
  const http = await service.getFacilitiesByURI(facilitiesUris) as any
  return http && http.response ? http.response.result : undefined
}

function facilityToSelectNode(dto: NamedResourceDTO) {
  if (!dto) return undefined
  return {
    label: dto.name,
    // shortUri needed to avoid auto deselection problem on selectors with both short and long URIs
    id: $opensilex.getShortUri(dto.uri)
  }
}

function onClear() {
  emit('clear')
}
</script>

<style scoped lang="scss"></style>

<i18n>
en:
  FacilitySelector:
    placeholder: Search and select a facility
    no-result: No facility found
fr:
  FacilitySelector:
    placeholder: "Rechercher et selectionner une installation"
    no-result: "Aucune installation trouvée"
</i18n>
