<template>
  <div>
    <div>
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
          <opensilex-FilterField :halfWidth="true">
            <opensilex-VariableDevicesSelector
                ref="devSelector"
                :devices.sync="filter.device"
                :variable="[variable]"
                label="DeviceDetails.title"
                :multiple="true"
                :showURI="false"
                @select="getTotalEventsCount"
                class="searchFilter"
            ></opensilex-VariableDevicesSelector>
          </opensilex-FilterField>
          </div>

          <div>
          <opensilex-FilterField :halfWidth="true">
            <div>
              <div>
                <opensilex-DateTimeForm
                    :value.sync="filter.startDate"
                    label="component.common.begin"
                    @input="onDateChange"
                    @clear="onDateChange"
                    class="searchFilter"
                ></opensilex-DateTimeForm>
              </div>
              <div>
                <opensilex-DateTimeForm
                    :value.sync="filter.endDate"
                    label="component.common.end"
                    name="endDate"
                    @input="onDateChange"
                    @clear="onDateChange"
                    class="searchFilter"
                ></opensilex-DateTimeForm>
              </div>
            </div>
          </opensilex-FilterField>
          </div>

          <opensilex-FilterField :halfWidth="true">
            <label>{{ $t("ScientificObjectVisualizationForm.show_events") }}</label>
            <b-form-checkbox v-model="filter.showEvents" switch>
              <b-spinner v-if="countIsLoading" small label="Busy"></b-spinner>
              <b-badge v-else variant="light">{{ $i18n.n(eventsCount) }}</b-badge>
            </b-form-checkbox>
          </opensilex-FilterField>
        </template>

        <template v-slot:advancedSearch>
          <opensilex-FilterField :halfWidth="true">
            <opensilex-DataProvenanceSelector
                ref="provSelector"
                :provenances.sync="filter.provenance"
                :targets="devices"
                label="Provenance"
                :multiple="false"
                :viewHandler="showProvenanceDetails"
                :viewHandlerDetailsVisible="visibleDetails"
                :showURI="false"
                @select="loadProvenance"
                @clear="clearProvenance"
            ></opensilex-DataProvenanceSelector>
          </opensilex-FilterField>

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
        </template>
      </opensilex-SearchFilterField>
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

let yesterday = new Date((new Date()).setHours(0, 0, 0, 0) - 1000 * 60 * 60 * 24)

@Component
export default class VariableVisualizationForm extends Vue {
  $opensilex: any;

  filterDeviceLabel: string = null;
  selectedDevices: any = [];
  selectedProvenance: any = null;
  filterProvenanceLabel: string = null;
  visibleDetails: boolean = false;
  countIsLoading: boolean = false;
  @Ref("searchField") readonly searchField!: any;

  filter = {
    variable: null,
    startDate: yesterday.toISOString(),
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
    this.$emit("search", this.filter);
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
        this.onUpdate();
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
