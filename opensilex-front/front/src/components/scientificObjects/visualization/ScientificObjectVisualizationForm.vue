<template>
  <div>
    <div class="card">
      <opensilex-SearchFilterField
        :withButton="true"
        :showTitle="true"
        @search="onSearch"
        @clear="clear"
        :showAdvancedSearch="true"
      >
        <template v-slot:filters>
          <!-- Type  -->
          <opensilex-FilterField :halfWidth="true">
            <opensilex-UsedVariableSelector
              label="ScientificObjectVisualizationForm.variable.label"
              :variables.sync="filter.variable"
              :multiple="false"
              :objects="[scientificObject]"
              :required="true"
              :clearable="true"
            ></opensilex-UsedVariableSelector>
          </opensilex-FilterField>

          <opensilex-FilterField :halfWidth="true">
            <div class="row">
              <div class="col col-xl-6 col-md-6 col-sm-6 col-12">
                <opensilex-DateTimeForm
                  :value.sync="filter.startDate"
                  label="component.common.begin"
                  name="startDate"
                  @input="getEvents"
                  @clear="getEvents"
                ></opensilex-DateTimeForm>
              </div>
              <div class="col col-xl-6 col-md-6 col-sm-6 col-12">
                <opensilex-DateTimeForm
                  :value.sync="filter.endDate"
                  label="component.common.end"
                  name="endDate"
                  @input="getEvents"
                  @clear="getEvents"
                ></opensilex-DateTimeForm>
              </div>
            </div>
          </opensilex-FilterField>

          <opensilex-FilterField :halfWidth="true">
            <label>{{ $t("ScientificObjectVisualizationForm.show_events") }}</label>
            <b-form-checkbox v-model="filter.showEvents" switch>
              <b-spinner
                v-if="countIsLoading" small   label="Busy"  ></b-spinner>
              <b-badge v-else variant="light">{{eventsCount}}</b-badge>
            </b-form-checkbox>
          </opensilex-FilterField>
        </template>

        <template v-slot:advancedSearch>
          <opensilex-FilterField :halfWidth="true">
            <opensilex-ProvenanceSelector
              ref="provSelector"
              :provenances.sync="filter.provenance"
              :filterLabel="filterProvenanceLabel"
              :scientificObject="scientificObject"
              label="Provenance"
              :multiple="false"
              :viewHandler="showProvenanceDetails"
              :viewHandlerDetailsVisible="visibleDetails"
              :showURI="false"
              @select="loadProvenance"
              @clear="clearProvenance"
            ></opensilex-ProvenanceSelector>
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
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import { EventGetDTO, ProvenanceGetDTO } from "opensilex-core/index";
// @ts-ignore
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";

@Component
export default class ScientificObjectVisualizationForm extends Vue {
  $opensilex: any;

  filterProvenanceLabel: string = null;
  selectedProvenance: any = null;
  visibleDetails: boolean = false;
  countIsLoading: boolean = false;

  filter = {
    variable: null,
    startDate: undefined,
    endDate: undefined,
    provenance: undefined,
    showEvents: false
  };

  resetFilters() {
    this.filter.variable = null;
    this.filter.startDate = undefined;
    this.filter.endDate = undefined;
    this.filter.provenance = undefined;
    this.filter.showEvents = false;
    this.filterProvenanceLabel = null;
  }

  @Prop()
  scientificObject;

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
