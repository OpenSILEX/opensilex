<template>
  <div>
    <div class="card">
      <opensilex-SearchFilterField ref="searchField" :withButton="false" :showAdvancedSearch="true">
        <template v-slot:filters>
          <!-- Type -->
          <opensilex-FilterField :halfWidth="true">
            <opensilex-ExperimentVariableSelector
              label="ExperimentDataVisuForm.search.variable.label"
              :variables.sync="filter.variable"
              :multiple="false"
              :experiment="selectedExperiment"
              :scientificObjects="scientificObjects"
              :clearable="true"
              :defaultSelectedValue="true"
              @select="onSearch"
            ></opensilex-ExperimentVariableSelector>
          </opensilex-FilterField>
          <opensilex-FilterField :halfWidth="true">
            <div class="row">
              <div class="col col-xl-6 col-md-6 col-sm-6 col-12">
                <opensilex-DateTimeForm
                  :value.sync="filter.startDate"
                  label="component.common.begin"
                  @input="onDateChange"
                  @clear="onDateChange"
                ></opensilex-DateTimeForm>
              </div>
              <div class="col col-xl-6 col-md-6 col-sm-6 col-12">
                <opensilex-DateTimeForm
                  :value.sync="filter.endDate"
                  label="component.common.end"
                  @input="onDateChange"
                  @clear="onDateChange"
                ></opensilex-DateTimeForm>
              </div>
            </div>
          </opensilex-FilterField>

          <opensilex-FilterField :halfWidth="true">
            <label>{{ $t("ScientificObjectVisualizationForm.show_events") }}</label>
            <b-form-checkbox v-model="filter.showEvents" @input="onUpdate" switch>
              <b-spinner v-if="false" small   label="Busy" ></b-spinner>
              <b-badge  v-if="false" variant="light">{{eventsCount}}</b-badge>
              </b-form-checkbox>
          </opensilex-FilterField>
        </template>

        <template v-slot:advancedSearch>
          <opensilex-FilterField :halfWidth="true">
            <opensilex-ProvenanceSelector
              ref="provSelector"
              :provenances.sync="filter.provenance"
              :filterLabel="filterProvenanceLabel"
              label="Provenance"
              @select="loadProvenance"
              @clear="clearProvenance"
              :experiment="selectedExperiment"
              :multiple="false"
              :viewHandler="showProvenanceDetails"
              :viewHandlerDetailsVisible="visibleDetails"
              :showURI="false"
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
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import { EventGetDTO, ProvenanceGetDTO } from "opensilex-core/index";
// @ts-ignore
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";

@Component
export default class ExperimentDataVisuForm extends Vue {
  $opensilex: any;

  showSearchComponent: boolean = false;
  filterProvenanceLabel: string = null;
  selectedProvenance: any = null;
  visibleDetails: boolean = false;
  countIsLoading : boolean = false;
  @Ref("searchField") readonly searchField!: any;
  @Ref("provSelector") readonly provSelector!: any;
  filter = {
    variable: undefined,
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
  selectedExperiment;

  @Prop()
  scientificObjects;

  eventsCountValue = "";
  public get eventsCount() {
    return this.eventsCountValue;
  }

  public set eventsCount(eventsCount: string) {
    this.eventsCountValue = eventsCount;
  }

  created() {
  //  this.getTotalEventsCount();
  }

  onDateChange(){
    this.getTotalEventsCount();
    this.onUpdate();
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
    this.scientificObjects.forEach((element, index) => {
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

  getEventsCount(os) {
    return this.$opensilex
      .getService("opensilex.EventsService")
      .searchEvents(
        undefined,
        this.filter.startDate != undefined && this.filter.startDate != ""
          ? this.filter.startDate
          : undefined,
        this.filter.endDate != undefined && this.filter.endDate != ""
          ? this.filter.endDate
          : undefined,
        os,
        undefined,
        undefined,
        0,
        1
      )
      .then((http: HttpResponse<OpenSilexResponse<Array<EventGetDTO>>>) => {
        return http.response.metadata.pagination.totalCount;
      });
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

<i18n>
en:
  ExperimentDataVisuForm:
     search:
       title: Search for data
       variable:
          label: Variable 
          placeholder: Search for a variable
       metadataKey : Metadata key
       metadataValue : Metadata value
fr:
  ExperimentDataVisuForm:
    search: 
       title: Recherche de données
       variable:
          label: Variable 
          placeholder: Saisir une variable
       metadataKey : Metadata key
       metadataValue : Metadata value

</i18n>
