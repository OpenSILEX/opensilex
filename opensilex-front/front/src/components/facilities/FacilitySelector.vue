<template>
  <InfiteScrollDropdown
    v-model:selected="facilitiesURIs"
    :fetchPage="searchFacilities"
    :placeholder="t(placeholder)"
    :noResultsText="t('FacilitySelector.no-result')"
    :conversionMethod="facilityToSelectNode"
    :itemLoadingMethod="loadFacilities"
    :multiple="multiple"
    :label="label"
    :helpMessage="helpMessage"
    :required="required"
    @selectionChange="emit('selectionChange')"
  ></InfiteScrollDropdown>
</template>

<script setup lang="ts">
import { inject } from 'vue'
import { useI18n } from 'vue-i18n'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type { OrganizationsService } from 'opensilex-core/api/organizations.service'
import type { NamedResourceDTO } from 'opensilex-core/index'
import type HttpResponse from 'opensilex-core/HttpResponse'
import type { OpenSilexResponse } from 'opensilex-core/HttpResponse'
import InfiteScrollDropdown from "@/components/common/forms/InfiteScrollDropdown.vue";

const { t } = useI18n()

const props = withDefaults(
  defineProps<{
    label?: string
    multiple?: boolean
    helpMessage?: string
    placeholder?: string
    required?: boolean
  }>(),
  {
    placeholder: 'FacilitySelector.placeholder',
    multiple: false,
    required: false
  }
)

const emit = defineEmits(['selectionChange'])

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!

const service = $opensilex.getService<OrganizationsService>('opensilex.OrganizationsService')

const facilitiesURIs = defineModel<string | string[] | null>('facilities');


async function searchFacilities(searchQuery: string, pageIndex: number, pageSize: number) {
  try {
    const http = await service.minimalSearchFacilities(
      searchQuery,
      undefined,
      ['name=asc'],
      pageIndex,
      pageSize
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
  return {
    label: dto.name,
    // shortUri needed to avoid auto deselection problem on selectors with both short and long URIs
    value: $opensilex.getShortUri(dto.uri)
  }
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
