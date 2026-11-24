<template>
  <!-- The list takes the full width until a facility is selected, then shares it with its description -->
  <n-grid :cols="selectedFacility ? '1 m:2' : 1" responsive="screen" :x-gap="16" :y-gap="16">
    <n-grid-item>
      <FacilitiesView
        ref="facilitiesView"
        :withActions="true"
        :isSelectable="true"
        @onUpdate="refresh"
        @onCreate="refresh"
        @onDelete="refresh"
        @facilitySelected="onFacilitiesViewFacilitySelected"
      />
    </n-grid-item>

    <n-grid-item v-if="selectedFacility">
      <FacilityDescription
        :selected="selectedFacility"
        :experiments="experiments"
      />
    </n-grid-item>
  </n-grid>
</template>

<script setup lang="ts">
import { inject, ref, useTemplateRef } from 'vue'
import { NGrid, NGridItem } from 'naive-ui'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type { FacilityGetDTO } from 'opensilex-core/index'
import type { OrganizationsService } from 'opensilex-core/api/organizations.service'
import type { ExperimentsService } from 'opensilex-core/api/experiments.service'
import type { ExperimentGetListDTO } from 'opensilex-core/model/experimentGetListDTO'
import FacilitiesView from '@/components/facilities/FacilitiesView.vue'
import FacilityDescription from '@/components/facilities/views/FacilityDescription.vue'

//#region Private

//#region Plugins & services
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const organizationsService = $opensilex.getService<OrganizationsService>('opensilex.OrganizationsService')
const experimentsService = $opensilex.getService<ExperimentsService>('opensilex.ExperimentsService')
//#endregion

//#region Template refs
const facilitiesView = useTemplateRef<InstanceType<typeof FacilitiesView>>('facilitiesView')
//#endregion

//#region Data & Computed
/** Facility shown in the description, with all its details */
const selectedFacility = ref<FacilityGetDTO>()
/** Ongoing experiments of the selected facility */
const experiments = ref<ExperimentGetListDTO[]>([])
//#endregion

//#region Event handlers
/**
 * Called when a row of the facility table is clicked (not its checkbox). The row only holds what the list shows,
 * so the full facility is fetched before being described.
 */
async function onFacilitiesViewFacilitySelected(facility: FacilityGetDTO) {
  experiments.value = []

  if (!facility?.uri) {
    selectedFacility.value = undefined
    return
  }

  try {
    const http = await organizationsService.getFacility(facility.uri)
    selectedFacility.value = http.response.result
    await loadExperiments(selectedFacility.value.uri)
  } catch (error) {
    $opensilex.errorHandler(error)
  }
}
//#endregion

//#region Methods
function refresh() {
  facilitiesView.value?.refresh()
}

/** Loads the first ongoing experiments hosted by the facility */
async function loadExperiments(facilityUri: string) {
  const http = await experimentsService.searchExperiments(
    undefined, // name
    undefined, // year
    false, // is_ended
    undefined, // species
    undefined, // factors
    undefined, // projects
    undefined, // is_public
    [facilityUri], // facilities
    undefined, // funding
    undefined, // order_by
    0,
    20
  )
  experiments.value = http.response.result
}
//#endregion

//#endregion
</script>
