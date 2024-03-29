<template>
  <div>
    <div>
      <opensilex-PageContent class="pagecontent">
  
        <!-- FILTERS -->
        <opensilex-SearchFilterField
            :withButton="true"
            searchButtonLabel="component.common.search.visualize-button"
            :showTitle="false"
            @search="onSearch"
            @clear="clear"
            :showAdvancedSearch="true"
            class="searchFilterField"
        >
          <template v-slot:filters>

            <!-- Variable -->
            <div>
              <opensilex-FilterField :halfWidth="true">
                <opensilex-VariableSelectorWithFilter
                    placeholder="VariableSelectorWithFilter.placeholder"
                    :variables.sync="filter.variable"
                    :devices="[device]"
                    :withAssociatedData="true"
                    maximumSelectedRows="1"
                    :required="true"
                    class="searchFilter"
                ></opensilex-VariableSelectorWithFilter>
              </opensilex-FilterField>
            </div>

            <!-- Scientific Objects -->
            <div>
              <opensilex-FilterField>
                <opensilex-UsedScientificObjectSelector
                  label="DataView.filter.scientificObjects"
                  placeholder="DataView.filter.scientificObjects-placeholder"
                  :scientificObjects.sync="filter.scientificObject"
                  :variables="filter.variable"
                  :devices="[device]"
                  :required="false"
                  :maximumSelectedRows="5"
                  class="searchFilter"
                ></opensilex-UsedScientificObjectSelector>
              </opensilex-FilterField>
            </div>

            <!-- Dates -->
            <div>
              <opensilex-FilterField :halfWidth="true">
                <div>

                  <opensilex-DateTimeForm
                      :value.sync="filter.startDate"
                      label="component.common.begin"
                      name="startDate"
                      :max-date="filter.endDate ? filter.endDate : undefined"
                      @input="getEvents"
                      @clear="getEvents"
                      class="searchFilter"
                  ></opensilex-DateTimeForm>
                </div>
                <div>
                  <opensilex-DateTimeForm
                      :value.sync="filter.endDate"
                      label="component.common.end"
                      name="endDate"
                      :min-date="filter.startDate ? filter.startDate : undefined"
                      :minDate="filter.startDate"
                      :maxDate="filter.endDate"
                      @input="getEvents"
                      @clear="getEvents"
                      class="searchFilter"
                  ></opensilex-DateTimeForm>

                </div>
              </opensilex-FilterField>
            </div>

            <!-- Events -->
            <div>
              <opensilex-FilterField :halfWidth="true">
                <label>{{ $t("ScientificObjectVisualizationForm.show_events") }}</label>
                <b-form-checkbox v-model="filter.showEvents" switch>
                  <b-spinner v-if="countIsLoading" small label="Busy"></b-spinner>
                  <b-badge v-else variant="light">{{ eventsCount }}</b-badge>
                </b-form-checkbox>
              </opensilex-FilterField>
            </div>
          </template>

          <template v-slot:advancedSearch>
            <!-- Provenance -->
            <opensilex-FilterField :halfWidth="true">
              <opensilex-DataProvenanceSelector
                  ref="provSelector"
                  :provenances.sync="filter.provenance"
                  :devices="[device]"
                  label="Provenance"
                  :multiple="false"
                  :viewHandler="showProvenanceDetails"
                  :viewHandlerDetailsVisible="visibleDetails"
                  @select="loadProvenance"
                  @clear="clearProvenance"
                  class="searchFilter"
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
      </opensilex-PageContent>
    </div>
  </div>
</template>

<script lang="ts">
import {Component, Prop } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import {
  EventsService,
  EventGetDTO,
  ProvenanceGetDTO
} from "opensilex-core/index";
// @ts-ignore
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";

let lastFifteenDays = new Date(new Date((new Date).setDate(new Date().getDate() - 15)).setHours(0,0,0,0))

@Component
export default class DeviceVisualizationForm extends Vue {
  $opensilex: any;

  filterProvenanceLabel: string = null;
  selectedProvenance: any = null;
  visibleDetails: boolean = false;
  countIsLoading: boolean = false;
  filter = {
    variable: [],
    scientificObject: [],
    startDate: lastFifteenDays.toISOString(),
    endDate: undefined,
    provenance: undefined,
    showEvents: false
  };

  resetFilters() {
    this.filter.variable = [];
    this.filter.scientificObject = [];
    this.filter.startDate = undefined;
    this.filter.endDate = undefined;
    this.filter.provenance = undefined;
    this.filter.showEvents = false;
    this.filterProvenanceLabel = null;
  }

  eventsService: EventsService;

  @Prop()
  device;

  @Prop()
  variables;

  eventsCountValue = "";

  public get eventsCount() {
    return this.eventsCountValue;
  }

  public set eventsCount(eventsCount: string) {
    this.eventsCountValue = eventsCount;
  }

  created() {
    this.getEvents();
  }

  getEvents() {
    this.$opensilex.disableLoader();
    this.countIsLoading = true;
    this.$opensilex
        .getService("opensilex.EventsService")
        .searchEvents(
            undefined,
            this.filter.startDate != undefined && this.filter.startDate != ""
                ? this.filter.startDate
                : undefined,
            this.filter.endDate != undefined && this.filter.endDate != ""
                ? this.filter.endDate
                : undefined,
            this.device,
            undefined,
            undefined,
            0,
            1
        )
        .then((http: HttpResponse<OpenSilexResponse<Array<EventGetDTO>>>) => {
          this.eventsCount = "" + http.response.metadata.pagination.totalCount;
          this.$opensilex.enableLoader();
          this.countIsLoading = false;
        })
        .catch(error => {
          this.$opensilex.enableLoader();
          this.countIsLoading = false;
        });
  }

  clear() {
    this.resetFilters();
    this.getEvents();
  }

  onSearch() {
    this.$emit("search", this.filter);
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
