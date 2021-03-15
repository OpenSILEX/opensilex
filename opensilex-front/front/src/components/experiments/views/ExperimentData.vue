<template>
  <div class="row">
    <div class="col col-xl-12">
      <opensilex-Card icon>
        <template v-slot:header>
          <br />
          <opensilex-CreateButton
            @click="dataForm.showCreateForm()"
            label="OntologyCsvImporter.import"
          ></opensilex-CreateButton>
        </template>
        <template v-slot:body>
          <opensilex-SearchFilterField
            ref="searchField"
            :withButton="true"
            label="DataView.filter.label"
            :showTitle="true"
            @search="updateFiltersAndSearch()"
            @clear="clear()"
          >
            <template v-slot:filters>
              <b-row>
              <opensilex-FilterField class="col col-xl-3 col-sm-6 col-12">
                <!-- Start Date -->
                <opensilex-DateTimeForm
                  :value.sync="filter.start_date"
                  label="component.experiment.startDate"
                  vid="startDate"
                  name="startDate"
                ></opensilex-DateTimeForm>
              </opensilex-FilterField>

              <opensilex-FilterField class="col col-xl-3 col-sm-6 col-12">
                <!-- End Date -->
                <opensilex-DateTimeForm
                  :value.sync="filter.end_date"
                  label="component.experiment.endDate"
                  name="endDate"
                ></opensilex-DateTimeForm>
              </opensilex-FilterField>

              <opensilex-FilterField :halfWidth="true">
                <opensilex-SelectForm
                  label="Variable"
                  :selected.sync="filter.variable"
                  :options="usedVariables"
                  placeholder="component.experiment.form.selector.variables.placeholder"
                ></opensilex-SelectForm>
              </opensilex-FilterField>
            </b-row>
            <b-row>
              <opensilex-FilterField :halfWidth="true">
                <opensilex-ProvenanceSelector
                  ref="provSelector"
                  :provenances.sync="filter.provenance"
                  :filterLabel="filterProvenanceLabel"
                  label="ExperimentData.provenance"
                  @select="loadProvenance"
                  @clear="filterLabel = null"
                  :experiment="uri"
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
                  <opensilex-ProvenanceDetails
                    :provenance="getSelectedProv"
                  ></opensilex-ProvenanceDetails>
                </b-collapse>
              </opensilex-FilterField>
            </b-row>
            </template>
          </opensilex-SearchFilterField>
          <opensilex-TableAsyncView
            v-if="searchVisible"
            ref="dataRef"
            :searchMethod="searchData"
            :fields="fields"
          >
            <template v-slot:export>
              <b-button class="mb-2 mr-2" @click="exportData('long')">{{
                $t("ExperimentData.export-long")
              }}</b-button>
              <b-button class="mb-2 mr-2" @click="exportData('wide')">{{
                $t("ExperimentData.export-wide")
              }}</b-button>
            </template>
            <template v-slot:cell(uri)="{ data }">
              {{ objects[data.item.scientific_objects[0]] }}
            </template>

            <template v-slot:cell(provenance)="{ data }">
              {{ provenances[data.item.provenance.uri] }}
            </template>
            <template v-slot:cell(variable)="{ data }">
              <opensilex-UriLink
                :uri="data.item.variable"
                :value="variables[data.item.variable]"
                :to="{
                  path:
                    '/variable/details/' +
                    encodeURIComponent(data.item.variable),
                }"
              ></opensilex-UriLink>
            </template>
            <template v-slot:cell(value)="{ data }">
              <div>{{ data.item.value }}</div>
            </template>
          </opensilex-TableAsyncView>
        </template>
      </opensilex-Card>
    </div>

    <opensilex-ModalForm
      ref="dataForm"
      :initForm="initFormData"
      createTitle="DatasetForm.create"
      editTitle="DatasetForm.update"
      component="opensilex-ExperimentDatasetForm"
      icon="fa#vials"
      modalSize="xl"
      @onCreate="afterCreateData"
      :successMessage="successMessage"
    ></opensilex-ModalForm>

    <opensilex-ResultModalView ref="resultModal"> </opensilex-ResultModalView>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import { ProvenanceGetDTO } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";

@Component
export default class ExperimentData extends Vue {
  $opensilex: any;
  $t: any;
  $route: any;
  uri = null;
  visibleDetails: boolean = false;
  searchVisible: boolean = false;
  usedVariables: any[] = [];
  selectedProvenance: any = null;

  filterProvenanceLabel: string = null;

  filter = {
    start_date: null,
    end_date: null,
    provenance: null,
    variable: null,
    mode: null,
  };

  @Ref("dataRef") readonly dataRef!: any;

  @Ref("dataForm") readonly dataForm!: any;

  @Ref("searchField") readonly searchField!: any;

  @Ref("provSelector") readonly provSelector!: any;

  @Ref("resultModal") readonly resultModal!: any;

  created() {
    this.uri = decodeURIComponent(this.$route.params.uri);
    let query: any = this.$route.query;

    this.resetFilters();
    for (let [key, value] of Object.entries(this.filter)) {
      if (query[key]) {
        this.filter[key] = decodeURIComponent(query[key]);
      }
    }
  }

  resetFilters() {
    this.filter = {
      provenance: null,
      variable: null,
      mode: null,
      start_date: null,
      end_date: null,
    };
    // Only if search and reset button are use in list
  }

  updateFiltersAndSearch(value: string) {
    for (let [key, value] of Object.entries(this.filter)) {
      this.$opensilex.updateURLParameter(key, value, "");
    }
    this.refresh();
  }

  successMessage(form) {
    return this.$t("ResultModalView.data-imported");
  }

  get getSelectedProv() {
    return this.selectedProvenance;
  }

  afterCreateData(results) {
    results.then((res) => {
      this.resultModal.setNbLinesImported(
        res.validation.dataErrors.nbLinesImported
      );
      this.resultModal.setProvenance(res.form.provenance);
      this.resultModal.show();
      this.filter.provenance = res.form.provenance.uri;
      this.refreshVariables();
      this.provSelector.refresh();
    });
  }

  initFormData(form) {
    form.experiment = this.uri;
    return form;
  }

  showProvenanceDetails() {
    if (this.selectedProvenance != null) {
      this.visibleDetails = !this.visibleDetails;
    }
  }

  exportData(mode: string) {
    let path =
      "/core/experiments/" + encodeURIComponent(this.uri) + "/data/export";
    let today = new Date();
    let filename =
      "export_data_" +
      today.getFullYear() +
      String(today.getMonth() + 1).padStart(2, "0") +
      String(today.getDate()).padStart(2, "0");
    this.filter.mode = mode;
    this.$opensilex.downloadFilefromService(path, filename, "csv", this.filter);
    this.filter.mode = null;
  }

  clear() {
    this.searchVisible = false;
    this.selectedProvenance = null;
    this.resetFilters()
    this.filterProvenanceLabel = null;
  }

  mounted() {
    this.searchVisible = false;
    this.refreshVariables();
  }

  refreshVariables() {
    this.$opensilex
      .getService("opensilex.ExperimentsService")
      .getUsedVariables(this.uri)
      .then((http) => {
        let variables = http.response.result;
        this.usedVariables = [];
        for (let i in variables) {
          let variable = variables[i];
          this.usedVariables.push({
            id: variable.uri,
            label: variable.name,
          });
        }
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
      this.getProvenance(selectedValue.id).then((prov) => {
        this.selectedProvenance = prov;
      });
    }
  }

  fields = [
    {
      key: "uri",
      label: "ExperimentData.object",
    },
    {
      key: "date",
      label: "ExperimentData.date",
       sortable: true
    },
    {
      key: "variable",
      label: "ExperimentData.variable",
       sortable: true
    },
    {
      key: "value",
      label: "ExperimentData.value",
       sortable: true
    },
    {
      key: "provenance",
      label: "ExperimentData.provenance",
       sortable: true
    },
  ];

  objects = {};
  variables = {};
  provenances = {};

  searchData(options) {
    let provUris = undefined;
    if (this.filter.provenance != null) {
      provUris = [this.filter.provenance];
    }

    let varUris = undefined;
    if (this.filter.variable != null) {
      varUris = [this.filter.variable];
    }
    let start_date = this.$opensilex.prepareGetParameter(
      this.filter.start_date
    );
    let end_date = this.$opensilex.prepareGetParameter(this.filter.end_date);

    return new Promise((resolve, reject) => {
      this.$opensilex
        .getService("opensilex.ExperimentsService")
        .searchExperimentDataList(
          this.uri,
          start_date, // start_date
          end_date, // end_date
          undefined, // timezone
          undefined, // objectUri
          varUris, // variableUri
          undefined, // min_confidence
          undefined, // max_confidence
          provUris, // provenance_uri
          undefined, // metadata
          options.orderBy, // order_by
          options.currentPage,
          options.pageSize
        )
        .then((http) => {
          let promiseArray = [];

          let objectToLoad = [];
          let variablesToLoad = [];
          let provenancesToLoad = [];
          if (http.response.result.length > 0) {
            for (let i in http.response.result) {
              let objectURI = http.response.result[i].scientific_objects[0];
              if (
                !this.objects[objectURI] &&
                !objectToLoad.includes(objectURI)
              ) {
                objectToLoad.push(objectURI);
              }
              let variableURI = http.response.result[i].variable;
              if (!variablesToLoad.includes(variableURI)) {
                variablesToLoad.push(variableURI);
              }
              let provenanceURI = http.response.result[i].provenance.uri;
              if (!provenancesToLoad.includes(provenanceURI)) {
                provenancesToLoad.push(provenanceURI);
              }
            }

            if (
              objectToLoad.length > 0 ||
              variablesToLoad.length > 0 ||
              provenancesToLoad.length > 0
            ) {
              if (objectToLoad.length > 0) {
                let promiseObject = this.$opensilex
                  .getService("opensilex.ScientificObjectsService")
                  .getScientificObjectsListByUris(this.uri, objectToLoad)
                  .then((httpObj) => {
                    for (let j in httpObj.response.result) {
                      let obj = httpObj.response.result[j];
                      this.objects[obj.uri] =
                        obj.name + " (" + obj.typeLabel + ")";
                    }
                  })
                  .catch(reject);
                promiseArray.push(promiseObject);
              }

              if (variablesToLoad.length > 0) {
                let promiseVariable = this.$opensilex
                  .getService("opensilex.VariablesService")
                  .getVariablesByURIs(variablesToLoad)
                  .then((httpObj) => {
                    for (let j in httpObj.response.result) {
                      let variable = httpObj.response.result[j];
                      this.variables[variable.uri] = variable.name;
                    }
                  })
                  .catch(reject);
                promiseArray.push(promiseVariable);
              }

              if (provenancesToLoad.length > 0) {
                let promiseProvenance = this.$opensilex
                  .getService("opensilex.DataService")
                  .getProvenancesByURIs(provenancesToLoad)
                  .then((httpObj) => {
                    for (let j in httpObj.response.result) {
                      let prov = httpObj.response.result[j];
                      this.provenances[prov.uri] = prov.name;
                    }
                    promiseArray.push(promiseProvenance);
                  })
                  .catch(reject);
              }
              Promise.all(promiseArray).then((values) => {
                resolve(http);
              });
            }
          } else {
            resolve(http);
          }
        })
        .catch(reject);
    });
  }

  refresh() {
    this.searchVisible = true;
    this.searchField.validatorRef.validate().then((isValid) => {
      if (isValid && this.dataRef) {
        this.dataRef.refresh();
      }
    });
  }
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
    ExperimentData:
        object: Scientific Object
        date: Date
        value: Value
        variable : Variable
        provenance: Provenance
        export-wide : Export data list (wide format)
        export-long : Export data list (long format)
fr:
    ExperimentData:
        object: Objet Scientifique
        date: Date
        value: Valeur
        variable : Variable
        provenance: Provenance
        export-wide : Exporter la liste de données (format large)
        export-long : Exporter la liste de données (format long)
</i18n>