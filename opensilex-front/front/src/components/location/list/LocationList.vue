<!--
  - ******************************************************************************
  -                         LocationList.vue
  - OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
  - Copyright © INRAE 2024.
  - Last Modification: 10/10/2024 13:28
  - Contact: alexia.chiavarino@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
  -
  -
  - ******************************************************************************
  -
  -->

<template>
  <div class="card">
    <opensilex-PageContent>
      <template v-slot>
        <div class="card-body">
          <opensilex-TableAsyncView
            ref="tableRef"
            :searchMethod="search"
            :fields="fields"
            defaultSortBy="endDate"
            :defaultSortDesc="true"
          >
            <template v-slot:cell(startDate)="{ data }">
              <opensilex-DateView :value="data.item.startDate"></opensilex-DateView>
            </template>

            <template v-slot:cell(endDate)="{ data }">
              <opensilex-DateView :value="data.item.endDate"></opensilex-DateView>
              <!-- Warning if the endDate is equal to 1970 == default date for facility geometry from migration-->
              <b-alert
                v-if="data.item.endDate === DEFAULT_DATE"
                variant="warning"
                show
                class="warning-box"
              >
                {{ $t("component.facility.warning.facility-default-date") }}
              </b-alert>
            </template>

            <template v-slot:cell(geometry)="{data}">
              <opensilex-GeometryCopy
                label="" :value="data.item.geojson">
              </opensilex-GeometryCopy>
            </template>
          </opensilex-TableAsyncView>
        </div>
      </template>
    </opensilex-PageContent>
  </div>
</template>

<script setup lang="ts">
import {inject} from 'vue';
import {LocationsService} from "opensilex-core/api/locations.service";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";


//#region Constant values and services
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!;
const locationsService: LocationsService = $opensilex.getService<LocationsService>('opensilex.LocationsService');
const fields = [
  {
    key: "startDate",
    label: "component.common.date-time-stuff.begin",
    sortable: true,
  },
  {
    key: "endDate",
    label: "component.common.date-time-stuff.end",
    sortable: true,
  },
  {
    key: "geometry",
    label: "component.common.geometry.geometry-title",
  }
];
const DEFAULT_DATE: string = "1970-01-01T00:00:00Z"
//endregion

//#region Props

interface Props {
  target: string
}
const props = defineProps<Props>();
//endregion

//#region functions
function search(options: { orderBy: any; currentPage: any; }) {
  return locationsService.searchLocationHistory(
    props.target,
    undefined,
    undefined,
    options.orderBy,
    options.currentPage,
    undefined
  );
}
//endregion
</script>

<style scoped lang="scss">

.warning-box {
  padding: 0.6rem;
  border-radius: 15px;
  text-align: center;
  max-width: fit-content;
}
</style>