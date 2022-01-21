<template>
  <div>
    <div class="card">
      <opensilex-SearchFilterField
          :withButton="true"
          searchButtonLabel="component.common.search.visualize-button"
          :showTitle="true"
          @search="onSearch"
          @clear="clear"
          :showAdvancedSearch="false"
      >

        <template v-slot:filters>
          <!-- Type -->
          <opensilex-FilterField :halfWidth="true">
            <opensilex-VariableDevicesSelector
                ref="devSelector"
                :devices.sync="filter.device"
                :variable="[variable]"
                label="Devices"
                :multiple="true"
                :showURI="false"
            ></opensilex-VariableDevicesSelector>
          </opensilex-FilterField>


          <opensilex-FilterField :halfWidth="true">
            <div class="row">
              <div class="col col-xl-6 col-md-6 col-sm-6 col-12">
                <opensilex-DateTimeForm
                    :value.sync="filter.startDate"
                    label="component.common.begin"
                    name="startDate"
                ></opensilex-DateTimeForm>
              </div>
              <div class="col col-xl-6 col-md-6 col-sm-6 col-12">
                <opensilex-DateTimeForm
                    :value.sync="filter.endDate"
                    label="component.common.end"
                    name="endDate"
                ></opensilex-DateTimeForm>
              </div>
            </div>
          </opensilex-FilterField>

        </template>

      </opensilex-SearchFilterField>
    </div>
  </div>
</template>

<script lang="ts">
import {Component, Prop} from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import {
  DeviceGetDTO
} from "opensilex-core/index";
// @ts-ignore
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";

@Component
export default class VariableVisualizationForm extends Vue {
  $opensilex: any;

  filterDeviceLabel: string = null;
  selectedDevices: any = [];
  visibleDetails: boolean = false;
  countIsLoading: boolean = false;
  filter = {
    variable: null,
    startDate: undefined,
    endDate: undefined,
    device: [],
  };

  resetFilters() {
    this.filter.variable = null;
    this.filter.startDate = undefined;
    this.filter.endDate = undefined;
    this.filter.device = [];
    this.filterDeviceLabel = null;
  }

  @Prop()
  variable;

  eventsCountValue = "";

  public get eventsCount() {
    return this.eventsCountValue;
  }

  public set eventsCount(eventsCount: string) {
    this.eventsCountValue = eventsCount;
  }

  created() {

  }

  clear() {
    this.resetFilters();
  }

  onSearch() {
    this.$emit("search", this.filter);
  }

}
</script>

<style scoped lang="scss">
.card-vertical-group {
  margin-bottom: 0px;
}
</style>