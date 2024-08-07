<template>
  <div>
    <div>
      <opensilex-PageContent class="pagecontent">
        <opensilex-SearchFilterField
            ref="searchField"
            :withButton="true"
            searchButtonLabel="component.common.search.visualize-button"
            :showTitle="false"
            @search="onSearch"
            @clear="clear"
            :showAdvancedSearch="true"
            class="searchFilterField"
        >
          <template v-slot:filters>
            <!-- Device -->
            <div>
              <opensilex-FilterField>
                <opensilex-VariableDevicesSelector
                    ref="devSelector"
                    :devices.sync="filter.device"
                    :variable="[variable]"
                    label="VariableVisualizationForm.devicesSelector"
                    :multiple="true"
                    :showURI="false"
                    @select="getTotalEventsCount"
                    @clear="getTotalEventsCount"
                    class="searchFilter"
                ></opensilex-VariableDevicesSelector>
              </opensilex-FilterField>
            </div>

            <div>
              <opensilex-FilterField>
                <div>
                  <div>
                    <!-- Start Date -->
                    <opensilex-DateTimeForm
                        :value.sync="filter.startDate"
                        label="component.common.begin"
                        :max-date="filter.endDate ? filter.endDate : undefined"
                        @input="onDateChange"
                        @clear="onDateChange"
                        class="searchFilter"
                    ></opensilex-DateTimeForm>
                  </div>
                  <div>
                    <!-- End Date -->
                    <opensilex-DateTimeForm
                        :value.sync="filter.endDate"
                        label="component.common.end"
                        name="endDate"
                        :min-date="filter.startDate ? filter.startDate : undefined"
                        :minDate="filter.startDate"
                        :maxDate="filter.endDate"
                        @input="onDateChange"
                        @clear="onDateChange"
                        class="searchFilter"
                    ></opensilex-DateTimeForm>
                  </div>
                </div>
              </opensilex-FilterField>
            </div>

            <!-- Events -->
            <div>
              <opensilex-FilterField>
                <label>{{ $t("ScientificObjectVisualizationForm.show_events") }}</label>
                <b-form-checkbox v-model="filter.showEvents" switch>
                  <b-spinner v-if="countIsLoading" small label="Busy"></b-spinner>
                  <b-badge v-else variant="light">{{ $i18n.n(eventsCount) }}</b-badge>
                </b-form-checkbox>
              </opensilex-FilterField>
            </div>
          </template>

          <template v-slot:advancedSearch>
            <!-- Provenance -->
            <opensilex-FilterField>
              <opensilex-DataProvenanceSelector
                  ref="provSelector"
                  label="Provenance"
                  :provenances.sync="filter.provenance"
                  :targets="devices"
                  :multiple="false"
                  :viewHandler="showProvenanceDetails"
                  :viewHandlerDetailsVisible="visibleDetails"
                  :showURI="false"
                  @select="loadProvenance"
                  @clear="clearProvenance"
                  class="searchFilter"
              ></opensilex-DataProvenanceSelector>
            </opensilex-FilterField>

            <div>
              <opensilex-FilterField>
                <b-collapse
                    v-if="selectedProvenance"
                    id="collapse-4"
                    v-model="visibleDetails"
                    class="mt-2"
                >
                  <opensilex-ProvenanceDetails :provenance="getSelectedProv"></opensilex-ProvenanceDetails>
                </b-collapse>
              </opensilex-FilterField>
            </div>
          </template>
        </opensilex-SearchFilterField>
      </opensilex-PageContent>
    </div>
  </div>
</template>

<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import {
  EventsService,
  EventGetDTO,
  ProvenanceGetDTO
} from "opensilex-core/index";
// @ts-ignore
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import VueI18n from "vue-i18n";

let lastFifteenDays = new Date(new Date((new Date).setDate(new Date().getDate() - 15)).setHours(0,0,0,0))

@Component
export default class VariableVisualizationForm extends Vue {
  $opensilex: any;

  filterDeviceLabel: string = null;
  selectedDevices: any = [];
  selectedProvenance: any = null;
  filterProvenanceLabel: string = null;
  visibleDetails: boolean = false;
  countIsLoading: boolean = false;
  $i18n: VueI18n;
  @Ref("searchField") readonly searchField!: any;

  filter = {
    variable: null,
    startDate: lastFifteenDays.toISOString(),
    endDate: undefined,
    device: [],
    provenance: undefined,
    showEvents: false
  };

  resetFilters() {
    this.filter.variable = null;
    this.filter.startDate = undefined;
    this.filter.endDate = undefined;
    this.filter.device = [];
    this.filter.provenance = undefined;
    this.filter.showEvents = false;
    this.filterDeviceLabel = null;

    this.filterProvenanceLabel = null;
  }

  @Prop()
  variable;

  @Prop()
  devices;

  eventsCountValue = "";

  public get eventsCount() {
    return this.eventsCountValue;
  }

  public set eventsCount(eventsCount: string) {
    this.eventsCountValue = eventsCount;
  }

  onDateChange() {
    this.getTotalEventsCount();
  }

  onUpdate() {
    this.$emit("update", this.filter);
  }

  onSearch() {
    if (this.filter.device && Array.isArray(this.filter.device) && this.filter.device.length > 0) {
      this.$emit("search", this.filter);
    } else {
      this.$opensilex.showInfoToast(
        this.$i18n.t("VariableVisualizationForm.deviceRequired")
      );
    }
  }

  getTotalEventsCount() {
    this.$opensilex.disableLoader();
    this.countIsLoading = true;
    let series = [],
        serie;
    let promises = [],
        promise;
    this.filter.device.forEach((element, index) => {
      promise = this.getEventsCount(element);
      promises.push(promise);
    });

    Promise.all(promises).then(values => {
      let count = 0;
      values.forEach(value => {
        if (value !== undefined) {
          count += value;
        }
      });
      this.eventsCount = "" + count;
      this.$opensilex.enableLoader();
      this.countIsLoading = false;

    }).catch(error => {
      this.$opensilex.enableLoader();
      this.countIsLoading = false;

    });
  }

  getEventsCount(device) {
    return this.$opensilex
        .getService("opensilex.EventsService")
        .searchEvents(
            undefined,
            this.filter.startDate != undefined
                ? this.filter.startDate
                : undefined,
            this.filter.endDate != undefined && this.filter.endDate != ""
                ? this.filter.endDate
                : undefined,
            [device],
            undefined,
            undefined,
            0,
            1
        )
        .then((http: HttpResponse<OpenSilexResponse<Array<EventGetDTO>>>) => {
          return http.response.metadata.pagination.totalCount;
        });
  }

  clear() {
    this.resetFilters();
    this.getTotalEventsCount();
  }

  getProvenance(uri) {
    if (uri != undefined && uri != null) {
      return this.$opensilex
          .getService("opensilex.DataService")
          .getProvenance(uri)
          .then((http: HttpResponse<OpenSilexResponse<ProvenanceGetDTO>>) => {
            return http.response.result;
          });
    }
  }

  loadProvenance(selectedValue) {
    if (selectedValue != undefined && selectedValue != null) {
      this.getProvenance(selectedValue.id).then(prov => {
        this.selectedProvenance = prov;
      });
    }
  }

  clearProvenance() {
    this.filterProvenanceLabel = null;
    this.onUpdate();
  }

  showProvenanceDetails() {
    if (this.selectedProvenance != null) {
      this.visibleDetails = !this.visibleDetails;
    }
  }

  get getSelectedProv() {
    return this.selectedProvenance;
  }
}
</script>

<style scoped lang="scss">
.card-vertical-group {
  margin-bottom: 0px;
}
</style>

<i18n>
en:
  VariableVisualizationForm:
    devicesSelector: Devices
    deviceRequired: "Please select at least one device."

fr:
  VariableVisualizationForm:
    devicesSelector: Appareils
    deviceRequired: "Veuillez selectionner au moins un appareil."
</i18n>