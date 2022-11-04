<template>
  <div>
    <div>
      <opensilex-PageContent class="pagecontent">

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
                <!-- Variables -->
                <div>
                  <opensilex-FilterField :halfWidth="true">
                    <opensilex-VariableSelectorWithFilter
                        placeholder="VariableSelectorWithFilter.placeholder"
                        :variables.sync="filter.variable"
                        :withAssociatedData="true"
                        :objects="[scientificObject]"
                        maximumSelectedRows="1"
                        :required="true"
                        class="searchFilter"
                    ></opensilex-VariableSelectorWithFilter>
                  </opensilex-FilterField>
                </div>

                <div>
                  <opensilex-FilterField :halfWidth="true">
                    <div>
                      <opensilex-DateTimeForm
                          :value.sync="filter.startDate"
                          label="component.common.begin"
                          name="startDate"
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
                          @input="getEvents"
                          @clear="getEvents"
                          class="searchFilter"
                      ></opensilex-DateTimeForm>
                    </div>
                  </opensilex-FilterField>
                </div>

                <div>
                  <opensilex-FilterField :halfWidth="true">
                    <label>{{ $t("ScientificObjectVisualizationForm.show_events") }}</label>
                    <b-form-checkbox v-model="filter.showEvents" switch>
                      <b-spinner
                          v-if="countIsLoading" small label="Busy"></b-spinner>
                      <b-badge v-else variant="light">{{ eventsCount }}</b-badge>
                    </b-form-checkbox>
                  </opensilex-FilterField>
                  <br>
                </div>
              </template>

              <template v-slot:advancedSearch>
                <opensilex-FilterField :halfWidth="true">
                  <opensilex-DataProvenanceSelector
                      ref="provSelector"
                      :provenances.sync="filter.provenance"
                      :targets="[scientificObject]"
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
import {Component, Prop} from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import {EventsService, EventGetDTO, ProvenanceGetDTO} from "opensilex-core/index";
// @ts-ignore
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import {DataService} from "opensilex-core/api/data.service";

@Component
export default class ScientificObjectVisualizationForm extends Vue {
  $opensilex: any;

  filterProvenanceLabel: string = null;
  selectedProvenance: any = null;
  visibleDetails: boolean = false;
  countIsLoading: boolean = false;
  dataService: DataService;

  filter = {
    variable: [],
    startDate: undefined,
    endDate: undefined,
    provenance: undefined,
    showEvents: false
  };

  resetFilters() {
    this.filter.variable = [];
    this.filter.startDate = undefined;
    this.filter.endDate = undefined;
    this.filter.provenance = undefined;
    this.filter.showEvents = false;
    this.filterProvenanceLabel = null;
  }

  @Prop()
  scientificObject;

  eventsCountValue = "";
  eventsService: EventsService;

  public get eventsCount() {
    return this.eventsCountValue;
  }

  public set eventsCount(eventsCount: string) {
    this.eventsCountValue = eventsCount;
  }

  created() {
    this.eventsService = this.$opensilex.getService("opensilex.EventsService");
    this.dataService = this.$opensilex.getService("opensilex.DataService");
    this.getEvents();
  }

  getEvents() {
    this.$opensilex.disableLoader();
    this.countIsLoading = true;
    this.eventsService
        .searchEvents(
        undefined,
        this.filter.startDate != undefined && this.filter.startDate != ""
            ? this.filter.startDate
            : undefined,
        this.filter.endDate != undefined && this.filter.endDate != ""
            ? this.filter.endDate
            : undefined,
        this.scientificObject,
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
      this.dataService
          .getProvenance(uri)
          .then((http: HttpResponse<OpenSilexResponse<ProvenanceGetDTO>>) => {
            this.selectedProvenance = http.response.result;
          });
    }
  }

  loadProvenance(selectedValue) {
    if (selectedValue != undefined && selectedValue != null) {
      this.getProvenance(selectedValue.id);
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

<i18n>
en:
  ScientificObjectVisualizationForm:
    show_events: Show Events
    variable:
      label: Variable
fr:
  ScientificObjectVisualizationForm:
    show_events: Voir les Evénements
    variable:
      label: Variable

</i18n>
