<template>
  <div>
    <div>
      <opensilex-PageContent class="pagecontent">
            <!-- Toggle Sidebar--> 
      <div class="searchMenuContainer"
      v-on:click="searchFiltersToggle = !searchFiltersToggle"
      :title="searchFiltersPannel()">
        <div class="searchMenuIcon">
          <i class="ik ik-search"></i>
        </div>
      </div>

       <!-- FILTERS -->
      <Transition>
        <div v-show="searchFiltersToggle">

      <opensilex-SearchFilterField
        ref="searchField"
        :withButton="true"
        searchButtonLabel="component.common.search.visualize-button"
        :showAdvancedSearch="true"
        class="searchFilterField visualizeBtn"
        @search="onSearch"
        @clear="resetFilters"
      >
        <template v-slot:filters>

          <!-- Scientific Objects -->
          <div>
            <opensilex-FilterField>
              <opensilex-UsedScientificObjectSelector
                label="DataView.filter.scientificObjects"
                placeholder="DataView.filter.scientificObjects-placeholder"
                :scientificObjects.sync="scientificObjectsURI"
                :soFilter="soFilter"
                :experiment="selectedExperiment"
                :required="true"
                :maximumSelectedRows="15"
                @input="onUpdate"
                @select="onSearch"
                @onValidate="onValidateScientificObjects"
                class="searchFilter"
              ></opensilex-UsedScientificObjectSelector>
            </opensilex-FilterField>
          </div>

          <!-- Variables -->
          <div>
            <opensilex-FilterField>
              <opensilex-VariableSelectorWithFilter
                label="ExperimentDataVisualisationForm.search.variable.label"
                placeholder="ExperimentDataVisualisationForm.search.variable.placeholder"
                :variables.sync="selectedVariables"
                :experiment="[selectedExperiment]"
                :withAssociatedData="true"
                :maximumSelectedRows="2"
                :objects="scientificObjects"
                :required="true"
                ref="variableRef"
                class="searchFilter"
              ></opensilex-VariableSelectorWithFilter>
            </opensilex-FilterField>
          </div>

          <div>
            <opensilex-FilterField>
              <!-- Begin date -->
              <div>
                <opensilex-DateTimeForm
                  :value.sync="filter.startDate"
                  label="component.common.begin"
                  @input="onUpdate"
                  @clear="onUpdate"
                  class="searchFilter"
                ></opensilex-DateTimeForm>
              </div>

              <!-- End date -->
              <div>
                <opensilex-DateTimeForm
                  :value.sync="filter.endDate"
                  label="component.common.end"
                  @input="onUpdate"
                  @clear="onUpdate"
                  class="searchFilter"
                ></opensilex-DateTimeForm>
              </div>
            </opensilex-FilterField>
          </div>

          <!-- Events -->
          <div>
            <opensilex-FilterField>
              <label>{{ $t("ScientificObjectVisualizationForm.show_events") }}</label>
              <b-form-checkbox v-model="filter.showEvents" switch>
                <b-spinner v-if="countIsLoading" small label="Busy" ></b-spinner>
                <b-badge  v-else variant="light">{{(eventsCount)}}</b-badge>
              </b-form-checkbox>
            </opensilex-FilterField>
          </div>
        </template>

        <!-- Provenance -->
        <template v-slot:advancedSearch>
          <opensilex-FilterField>
            <opensilex-DataProvenanceSelector
              ref="provSelector"
              :provenances.sync="filter.provenance"
              label="Provenance"
              @select="loadProvenance"
              @clear="clearProvenance"
              :experiments="[selectedExperiment]"
              :targets="scientificObjects"
              :multiple="false"
              :viewHandler="showProvenanceDetails"
              :viewHandlerDetailsVisible="visibleDetails"
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
              <opensilex-ProvenanceDetails
                :provenance="getSelectedProv"
              >
              </opensilex-ProvenanceDetails>
            </b-collapse>          
          </opensilex-FilterField>
          </div>
        </template>
      </opensilex-SearchFilterField>
        </div>
      </Transition>
      </opensilex-PageContent>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, PropSync, Ref, Watch } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import { EventGetDTO, ProvenanceGetDTO } from "opensilex-core/index";
// @ts-ignore
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
import { ScientificObjectsService } from "opensilex-core/index";
import ScientificObjectList from "../../scientificObjects/ScientificObjectList.vue";

@Component
export default class ExperimentDataVisualisationForm extends Vue {
  $opensilex: any;
  soService: ScientificObjectsService;
  showSearchComponent: boolean = false;
  filterProvenanceLabel: string = null;
  selectedProvenance: any = null;
  visibleDetails: boolean = false;
  countIsLoading : boolean = false;

  @Ref("searchField") readonly searchField!: any;
  @Ref("provSelector") readonly provSelector!: any;
  @Ref("variableRef") readonly variableRef!: any;

  filter = {
    scientificObject: [],
    variable: [],
    startDate: undefined,
    endDate: undefined,
    provenance: undefined,
    showEvents: false
  };

  resetFilters() {
    this.filter.scientificObject = [];
    this.filter.variable = [];
    this.filter.startDate = undefined;
    this.filter.endDate = undefined;
    this.filter.provenance = undefined;
    this.filter.showEvents = false;
    this.filterProvenanceLabel = null;
    
    // those filters are not handled as the others
    this.scientificObjectsURI = [];
    this.selectedVariables = [];

  }

  @Prop()
  selectedExperiment;

  @PropSync("scientificObjects")
  scientificObjectsURI;

  @Prop()
  scientificObjects;

  @Prop()
  soFilter;

  @Prop()
  refreshSoSelector;

  @PropSync("selectedVar")
  selectedVariables;

  get startDate() {
    return this.filter.startDate
  }

  get endDate() {
    return this.filter.endDate
  }

  eventsCount = "";

 
    searchFiltersToggle = true;


  searchFiltersPannel() {
    return  this.$t("searchfilter.label")
  }

  onUpdate() {
    this.$emit("update", this.filter);
    this.getTotalEventsCount();
  }

  onSearch() {
    this.getTotalEventsCount();
    this.$emit("search", this.filter);
    this.searchFiltersToggle = false;
  }

    //  search events on EventsService format
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
          // return a response for each OS selected = to the number of events associated to this OS.
          return http.response.metadata.pagination.totalCount;
        });
    }

  getTotalEventsCount() {
    this.$opensilex.disableLoader();
    this.countIsLoading = true;
    let series = [],
      serie;
    let promises = [],
      promise;

    // for each OS  store the promisse resulting of getEventsCount() in [promises]
    this.scientificObjectsURI.forEach((element) => {    
      promise = this.getEventsCount(element);
      promises.push(promise);
    });
    // after all promises are fulfilled, if values, increment count for each one of them
    Promise.all(promises).then(values => {
      let count = 0;
      values.forEach(value => {
        if (value !== undefined) {
          count += value;
        }
      })
      //eventsCount (cf badge) take the value of the number from events count
      this.eventsCount = "" + count;  
      this.$opensilex.enableLoader();
      this.countIsLoading = false;
    }).catch(error => {
      this.$opensilex.enableLoader();
      this.countIsLoading = false;
    });
      return this.eventsCount;
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


// waits for the URI value change (OS are selected) to start function
@Watch("scientificObjectsURI")
  refreshVariablesSelector() {
    this.getTotalEventsCount();
  }

  onValidateScientificObjects(selection) {
    this.variableRef.refreshVariableSelector();
    this.$emit("onValidateScientificObjects", selection);
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
  ExperimentDataVisualisationForm:
     search:
       title: Search for data
       variable:
          label: Variable(s) 
          placeholder: Select variables
       metadataKey : Metadata key
       metadataValue : Metadata value
fr:
  ExperimentDataVisualisationForm:
    search: 
       title: Recherche de données
       variable:
          label: Variable(s) 
          placeholder: Sélectionner des variables
       metadataKey : Metadata key
       metadataValue : Metadata value

</i18n>
