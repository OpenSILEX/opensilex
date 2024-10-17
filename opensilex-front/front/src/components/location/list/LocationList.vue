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

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import {Prop, Ref} from "vue-property-decorator";
import LocationModalForm from "../../location/form/LocationModalForm.js";
import {LocationsService} from "opensilex-core/api/locations.service";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";

@Component({})
export default class LocationList extends Vue {
  //#region Plugins and services
  private readonly $opensilex: OpenSilexVuePlugin;
  private locationsService: LocationsService;
  //endregion

  //#region Props
  @Prop({default: () => [] })
  private readonly locations;
  @Prop()
  private readonly target;
  //endregion

  //#region Refs
  @Ref("locationModalForm")
  private readonly locationModalForm: LocationModalForm;
  //endregion

  //#region Data
  private fields = [
    {
      key: "startDate",
      label: "component.common.begin",
      sortable: true,
    },
    {
      key: "endDate",
      label: "component.common.end",
      sortable: true,
    },
    {
      key: "geometry",
      label: "component.common.geometry",
    }
  ]
  //endregion

  //#region Computed
  //endregion

  //#region Events
  //endregion

  //#region Events handlers
  //endregion

  //#region Public methods
  //endregion

  //#region Hooks
  created() {
    this.locationsService = this.$opensilex.getService<LocationsService>("opensilex.LocationsService")
  }
  //endregion

  //#region Private methods
  private search(options) {
    return this.locationsService.searchLocationHistory(
        this.target,
        undefined,
        undefined,
        options.orderBy,
        options.currentPage,
        undefined
    );
  }
  //endregion
}
</script>

<style scoped lang="scss">

</style>